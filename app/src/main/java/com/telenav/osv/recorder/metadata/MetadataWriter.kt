package com.telenav.osv.recorder.metadata

import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.telenav.osv.item.KVFile
import com.telenav.osv.recorder.metadata.callback.MetadataWrittingStatusCallback
import com.telenav.osv.utils.Utils
import timber.log.Timber
import java.io.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.GZIPOutputStream

/**
 * Class which handles all metadata write requirements. It will hold a queue in order to only write a maximum number of rows since the write is quite expensive.
 *
 * There are three methods available for the writer:
 * * [createFile]
 * * [appendInFile]
 * * [finish]
 *
 * The recommended lifecycle is to call the [createFile] method, followed by any number of [appendInFile] and once the writing is no longer necessary it is required for [finish] to be called.
 */
class MetadataWriter {

    private var outputStream: FileOutputStream? = null

    private var bufferedWriter: BufferedWriter? = null

    private var metadataFile: KVFile? = null

    private val sensorDataQueue = ConcurrentLinkedQueue<String>()

    private val bufferedLines = AtomicInteger()

    private val finishInProgress = AtomicBoolean()

    private val createInProgress = AtomicBoolean()

    private var mBackgroundThread: HandlerThread? = null

    private var mBackgroundHandler: Handler? = null

    private lateinit var parentFolder: KVFile

    /**
     * Callback listener which can be set in order to obtain data for either success/failure in writing the metadata.
     */
    private var metadataCallback: MetadataWrittingStatusCallback? = null

    /**
     * Method used to add data to the existent queue. There is a special use case where after [finish] was called there won't be any new addition to the queue.
     *
     * Based on the [flushNow] param this will either check internally if the queue has reached the [MAX_BUFFERED_LINES] and trigger
     * a flush on the disk or trigger the flush on the disk disregarding the queue length.
     * *Note that is method has a synchronization lock to avoid different rows entry position modification since the way the data is being processed should be in the order it is provided.
     * @param data the encoded data to be written in the metadata.
     * @param flushNow the optional parameter which will trigger an immediate data flush on the disk with all the cached data including the [data] param.
     */
    fun appendInFile(data: String, flushNow: Boolean = false) {
        Timber.d("appendInFile. Finish in progress: ${finishInProgress.get()}. Flush now: $flushNow. Data: $data")
        // if the finish was in progress new data is locked since it will be put under the END mark which will make the parser not able to finish the sequence
        if (!createInProgress.get() && !finishInProgress.get()) {
            sensorDataQueue.add(data)
            if (flushNow) {
                flushToDisk()
            } else {
                flushIfNeeded()
            }
        }
    }

    /**
     * Method which will signal and final entry in the metadata file which will also compress the file into a .gz file.
     */
    fun finish(endData: String) {
        sensorDataQueue.add(endData)
        finishInProgress.set(true)
        flushToDisk(true)
    }

    /**
     * Method which will create a new metadata file in the specified [parentFolder]. This also required the header data and the listener for the callback since all the processing is done in the background.
     * @param parentFolder the folder where the metadata file/compressed file will be created in
     * @param metadataCallback the callback listener to signal finish/error responses
     * @param header the header data for the metadata
     */
    fun createFile(parentFolder: KVFile, metadataCallback: MetadataWrittingStatusCallback, header: String) {
        prepareForMetadataCreate()
        //append the header portion
        sensorDataQueue.add(header)
        bufferedLines.incrementAndGet()
        this.metadataCallback = metadataCallback
        createBackgroundHandlerIfNecessary()
        mBackgroundHandler?.post {
            val newFile = KVFile(parentFolder, FILE_NAME)
            //check if the file exists, more a fail-safe than anything
            if (!newFile.exists()) {
                try {
                    newFile.createNewFile()
                    outputStream = FileOutputStream(newFile, true)
                    bufferedWriter = outputStream!!.bufferedWriter()
                } catch (e: Exception) {
                    Timber.d("createFile. Status: ${e.message}.")
                    val crashlytics = FirebaseCrashlytics.getInstance()
                    crashlytics.recordException(e)
                    closeResources()
                    this.metadataCallback?.onMetadataLoggingError(e)
                    return@post
                }
            }
            this.metadataFile = newFile
            this.parentFolder = parentFolder
            createInProgress.set(false)
            this.metadataCallback?.onMetadataCreated()
        }
    }

    private fun prepareForMetadataCreate() {
        createInProgress.set(true)
        //fail-safe to remove data in queue
        if (!sensorDataQueue.isEmpty()) {
            Timber.d("createFile. Status: sensor data queue not empty. Message: Clearing sensor data queue")
            sensorDataQueue.clear()
        }
        //reset the count for buffered lines
        bufferedLines.set(0)
        //fail-safe to reset finis in progress mark
        finishInProgress.set(false)
    }

    private fun flushIfNeeded() {
        val count = bufferedLines.incrementAndGet()
        Timber.d("flushIfNeeded. Status: buffered lines. No: $count")
        if (count >= MAX_BUFFERED_LINES) {
            flushToDisk()
        }
    }

    private fun flushToDisk(zipLog: Boolean = false) {
        mBackgroundHandler?.post {
            Timber.d("flushToDisk. Status: starting disk write")
            //append all lines from the data queue to the buffered writer
            while (!sensorDataQueue.isEmpty()) {
                try {
                    //get the element but do not remove it if the writing fails
                    bufferedWriter?.write(sensorDataQueue.peek())
                    //remove the element at the head of the queue since the write operation was successful
                    sensorDataQueue.poll()
                } catch (exception: IOException) {
                    val exceptionLog = "flushToDisk. Zip file: {$zipLog}. Error: ${exception.message}"
                    Timber.d(exceptionLog)
                    val crashlytics = FirebaseCrashlytics.getInstance()
                    crashlytics.recordException(exception)
                    if (!zipLog) {
                        //signal error therefore metadata will close so there is no reason to add new sensor data
                        finishInProgress.set(true)
                        metadataCallback?.onMetadataLoggingError(exception)
                        return@post
                    } else {
                        //stop the while since by crashing it did not get to sensorDataQueue.poll() and has no way to leave the while
                        break
                    }
                }
            }
            //the compression part where if the param signals a finish will compress the file
            if (zipLog) {
                zipFile()
            }
            //rest the writing counter for the flushIfNeeded button
            bufferedLines.set(0)
        }
    }

    private fun zipFile() {
        closeResources()
        //for the cases when there was not anything written in the metadata but the file was created into the memory but not on the physical disk
        if (metadataFile != null && metadataFile!!.exists()) {
            val zipFile = KVFile(parentFolder, FILE_ZIP_NAME)
            //generate a .gz compress file with the text
            GZIPOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { out ->
                //denote the size of the array used to read data
                val data = ByteArray(SIZE_BYTE_ARRAY)
                //fail-safe
                metadataFile?.let { metadataFile ->
                    FileInputStream(metadataFile).use { fi ->
                        BufferedInputStream(fi).use { origin ->
                            //the try-catch mechanism to signal the finish callback if the metadata compression was wrong since we can upload the .txt file also
                            try {
                                while (true) {
                                    val readBytes = origin.read(data)
                                    if (readBytes == INVALID_BYTE_ARRAY_SIZE) {
                                        //break the current loop which is infinite
                                        break
                                    }
                                    out.write(data, 0, readBytes)
                                }
                                metadataCallback?.onMetadataLoggingFinished(Utils.fileSize(zipFile))
                                //remove the text file since it is no longer required
                                metadataFile.delete()
                            } catch (e: IOException) {
                                val crashlytics = FirebaseCrashlytics.getInstance()
                                crashlytics.recordException(e)
                                //remove the zip file since it is corrupt and not usable
                                zipFile.delete()
                                metadataCallback?.onMetadataLoggingFinished(Utils.fileSize(metadataFile))
                            }
                            finishInProgress.set(false)
                            this.metadataFile = null
                        }
                    }
                }
            }
        } else {
            finishInProgress.set(false)
            //call a metadata finish with the size set on 0 since there is no physical file on the disk
            metadataCallback?.onMetadataLoggingFinished(0)
        }
    }

    /**
     * Cleans up the resources by closing all the input streams.
     */
    private fun closeResources() {
        sensorDataQueue.clear()
        bufferedWriter?.close()
        outputStream?.close()
        bufferedWriter = null
        outputStream = null
    }

    private fun createBackgroundHandlerIfNecessary() {
        if (mBackgroundHandler == null || mBackgroundThread == null || !mBackgroundThread!!.isAlive) {
            mBackgroundThread = HandlerThread(NAME_HANDLER_THREAD, Process.THREAD_PRIORITY_FOREGROUND)
            mBackgroundThread!!.start()
            mBackgroundHandler = Handler(mBackgroundThread!!.looper)
        }
    }

    private companion object {
        /**
         * Name for the text metadata file
         */
        const val FILE_NAME = "/track.txt"

        /**
         * Name for the compressed metadata file
         */
        const val FILE_ZIP_NAME = "track.txt.gz"

        /**
         * Maximum number of values in the [sensorDataQueue] before an automatic trigger of a disk flush.
         */
        const val MAX_BUFFERED_LINES = 40

        /**
         * The size for the array used to read data
         */
        const val SIZE_BYTE_ARRAY = 1024

        const val NAME_HANDLER_THREAD = "SensorCollector"

        const val INVALID_BYTE_ARRAY_SIZE = -1
    }
}