package com.telenav.osv.recorder.metadata.callback

interface MetadataWrittingStatusCallback {

    /**
     * Callback which signals that metadata file is created
     */
    fun onMetadataCreated()

    /**
     * Callback received when an error was thrown during metadata writing.
     */
    fun onMetadataLoggingError(e: Exception?)

    /**
     * Callback received when metadata logging was finished and the zipped file was created.
     * @param metadataSize `long` which represents the size of the metadata, either the `.txt` file or `.zip` file.
     */
    fun onMetadataLoggingFinished(metadataSize: Long)
}