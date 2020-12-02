package com.telenav.osv.data.database

import android.content.Context
import android.content.res.Resources
import android.location.Location
import com.telenav.osv.data.frame.database.entity.FrameEntity
import com.telenav.osv.data.frame.database.entity.FrameWithLocationEntity
import com.telenav.osv.data.frame.model.Frame
import com.telenav.osv.data.location.database.entity.LocationEntity
import com.telenav.osv.data.location.model.KVLocation
import com.telenav.osv.data.score.database.entity.ScoreEntity
import com.telenav.osv.data.score.datasource.ScoreDataSource
import com.telenav.osv.data.score.model.ScoreHistory
import com.telenav.osv.data.sequence.database.entity.SequenceEntity
import com.telenav.osv.data.sequence.database.entity.SequenceWithRewardEntity
import com.telenav.osv.data.sequence.model.LocalSequence
import com.telenav.osv.data.sequence.model.details.SequenceDetails
import com.telenav.osv.data.sequence.model.details.SequenceDetailsLocal
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionJpeg
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionVideo
import com.telenav.osv.data.sequence.model.details.reward.SequenceDetailsRewardPoints
import com.telenav.osv.data.video.database.entity.VideoEntity
import com.telenav.osv.data.video.model.Video
import com.telenav.osv.item.KVFile
import junit.framework.Assert.*
import net.danlew.android.joda.JodaTimeAndroid
import org.joda.time.DateTime
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import java.io.InputStream
import java.util.*
import kotlin.test.assertTrue

/**
 * @author horatiuf
 */
class DataConverterTest {

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var scoreDataSource: ScoreDataSource

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        val appContext = mock(Context::class.java)
        val resources = mock(Resources::class.java)
        `when`(resources.openRawResource(anyInt())).thenReturn(mock(InputStream::class.java))
        `when`(appContext.resources).thenReturn(resources)
        `when`(context.applicationContext).thenReturn(appContext)
        JodaTimeAndroid.init(context)
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
    }

    @Test
    fun toScoreEntityWithNullableFields() {
        val scoreHistory = generateScoreHistory()
        val sequenceId = UUID.randomUUID().toString()

        val scoreEntity = DataConverter.toScoreEntity(scoreHistory, sequenceId)
        assertScoreEntityNonNullables(scoreEntity, scoreHistory)
        assertNotNull(scoreEntity.obdFrameCount)
        assertEquals(scoreEntity.obdFrameCount as Int, scoreHistory.obdPhotoCount)
        assertNotNull(scoreEntity.frameCount)
        assertEquals(scoreEntity.frameCount as Int, scoreHistory.photoCount)
    }

    @Test
    fun toScoreHistoryWithoutNullableFields() {
        val scoreEntity = generateScoreEntity(true)

        val scoreHistory = DataConverter.toScoreHistory(scoreEntity)

        assertScoreEntityNonNullables(scoreEntity, scoreHistory)
        assertEquals(scoreHistory.obdPhotoCount, 0)
        assertEquals(scoreHistory.photoCount, 0)
    }

    @Test
    fun toScoreHistory() {
        val scoreEntity = generateScoreEntity(false)

        val scoreHistory = DataConverter.toScoreHistory(scoreEntity)

        assertScoreEntityNonNullables(scoreEntity, scoreHistory)
        assertNotNull(scoreEntity.obdFrameCount)
        assertEquals(scoreEntity.obdFrameCount as Int, scoreHistory.obdPhotoCount)
        assertNotNull(scoreEntity.frameCount)
        assertEquals(scoreEntity.frameCount as Int, scoreHistory.photoCount)
    }

    @Test
    fun toFrameEntity() {
        val frame = generateFrame(false)
        val seqID = UUID.randomUUID().toString()

        val frameEntity = DataConverter.toFrameEntity(frame, seqID)

        assertFrameWithNonNullables(frameEntity, frame)
        assertNotNull(frame.location)
        assertEquals(seqID, frameEntity.sequenceID)
    }

    @Test
    fun toFrameEntityWithNullableFields() {
        val frame = generateFrame(true)
        val seqID = UUID.randomUUID().toString()

        val frameEntity = DataConverter.toFrameEntity(frame, seqID)

        assertFrameWithNonNullables(frameEntity, frame)
        assertNull(frame.location)
        assertEquals(seqID, frameEntity.sequenceID)
    }

    @Test
    fun toFrameWithoutNullableFields() {
        val frameEntity = generateFrameEntity(true)

        val frame = DataConverter.toFrame(frameEntity)

        assertFrameWithNonNullables(frameEntity, frame)
        assertNull(frameEntity.dateTime)
        assertEquals(frameEntity.dateTime, frame.dateTime)
        assertNotNull(frameEntity.frameId)
        assertNotNull(frameEntity.filePath)
        assertNotNull(frameEntity.index)
        assertNotNull(frameEntity.sequenceID)
    }

    @Test
    fun toVideoEntity() {
        val video = generateVideo()
        val seqID = UUID.randomUUID().toString()

        val videoEntity = DataConverter.toVideoEntity(video, seqID)

        assertVideoEntityWithNonNullables(videoEntity, video)
        assertEquals(seqID, videoEntity.sequenceID)
    }

    @Test
    fun toLocationEntity() {
        val osvLocation = generateKvLocation()
        val videoID = UUID.randomUUID().toString()
        val frameID = UUID.randomUUID().toString()

        val locationEntity = DataConverter.toLocationEntity(osvLocation, videoID, frameID)

        assertLocationWithNonNullables(locationEntity, osvLocation)
        assertNotNull(locationEntity.videoID)
        assertNotNull(locationEntity.frameID)
    }

    @Test
    fun toLocationEntityWithNullIds() {
        val osvLocation = generateKvLocation()

        val locationEntity = DataConverter.toLocationEntity(osvLocation, null, null)

        assertLocationWithNonNullables(locationEntity, osvLocation)
        assertNull(locationEntity.videoID)
        assertNull(locationEntity.frameID)
    }

    @Test
    fun toLocationEntityWithoutNullable() {
        val locationEntity = generateLocationEntity(true)

        val osvLocation = DataConverter.toKVLocation(locationEntity)

        assertLocationWithNonNullables(locationEntity, osvLocation)
        assertNull(locationEntity.videoID)
        assertNull(locationEntity.frameID)
    }

    @Test
    fun toFrame() {
        val frameEntity = generateFrameEntity(false)

        val frame = DataConverter.toFrame(frameEntity)

        assertFrameWithNonNullables(frameEntity, frame)
        assertEquals(frameEntity.dateTime, frame.dateTime)
        assertNotNull(frameEntity.frameId)
        assertNotNull(frameEntity.filePath)
        assertNotNull(frameEntity.index)
        assertNotNull(frameEntity.sequenceID)
    }

    @Test
    fun toVideo() {
        val videoEntity = generateVideoEntity()

        val video = DataConverter.toVideo(videoEntity)

        assertVideoEntityWithNonNullables(videoEntity, video)
        assertNotNull(videoEntity.filePath)
        assertNotNull(videoEntity.frameCount)
        assertNotNull(videoEntity.index)
        assertNotNull(videoEntity.sequenceID)
        assertNotNull(videoEntity.videoId)
    }

    @Test
    fun toLocations() {
        val osvLocations = (1..10).map { generateKvLocation() }

        val androidLocations = DataConverter.toLocations(osvLocations)

        assertFalse(androidLocations.isEmpty())
        for (item in androidLocations) assertTrue(item is Location)
    }

    @Test
    fun toLocationsEmptyInput() {
        val androidLocations = DataConverter.toLocations(listOf<KVLocation>())
        assertTrue(androidLocations.isEmpty())
    }

    @Test
    fun toKVLocation() {
        val locationEntity = generateLocationEntity(false)

        val osvLocation = DataConverter.toKVLocation(locationEntity)

        assertLocationWithNonNullables(locationEntity, osvLocation)
        assertNotNull(locationEntity.videoID)
        assertNotNull(locationEntity.frameID)
    }

    @Test
    fun toSequenceEntityWithVideoAndPoints() {
        val localSequence = generateLocalSequence(nullableFields = false, videoCompression = true, paid = false)

        val sequenceEntity = DataConverter.toSequenceEntity(localSequence)

        assertSequenceEntityNonNullables(sequenceEntity, localSequence)
        assertTrue(localSequence.compressionDetails is SequenceDetailsCompressionVideo)
        assertTrue(localSequence.rewardDetails is SequenceDetailsRewardPoints)
    }

    @Test
    fun toSequenceEntityWithJpegAndPoints() {
        val localSequence = generateLocalSequence(nullableFields = false, videoCompression = false, paid = false)

        val sequenceEntity = DataConverter.toSequenceEntity(localSequence)

        assertSequenceEntityNonNullables(sequenceEntity, localSequence)
        assertTrue(localSequence.compressionDetails is SequenceDetailsCompressionJpeg)
        assertTrue(localSequence.rewardDetails is SequenceDetailsRewardPoints)
    }

    @Test
    fun toSequenceEntityWithVideoAndNullables() {
        val localSequence = generateLocalSequence(nullableFields = true, videoCompression = false)

        val sequenceEntity = DataConverter.toSequenceEntity(localSequence)

        assertSequenceEntityNonNullables(sequenceEntity, localSequence)
        assertTrue(localSequence.compressionDetails is SequenceDetailsCompressionJpeg)
        assertNull(localSequence.rewardDetails)
    }

    @Test
    fun toSequenceEntityWithJpegAndNullables() {
        val localSequence = generateLocalSequence(nullableFields = true, videoCompression = true)

        val sequenceEntity = DataConverter.toSequenceEntity(localSequence)

        assertSequenceEntityNonNullables(sequenceEntity, localSequence)
        assertTrue(localSequence.compressionDetails is SequenceDetailsCompressionVideo)
        assertNull(localSequence.rewardDetails)
    }

    @Test
    fun toLocalSequenceWithVideoCompresion() {
        var videoCount = 5
        val sequenceEntity = generateSequenceEntity(false, videoCount)

        val localSequence = DataConverter.toLocalSequence(sequenceEntity)

        assertSequenceEntityNonNullables(sequenceEntity, localSequence)
        assertEquals(sequenceEntity.isObd, localSequence.details.isObd)
        assertNotNull(sequenceEntity.addressName)
        assertEquals(sequenceEntity.addressName, localSequence.details.addressName)
        assertNotNull(sequenceEntity.videoCount)
        assertEquals(sequenceEntity.videoCount, videoCount)
        assertEquals(sequenceEntity.videoCount, localSequence.compressionDetails.length)
        assertEquals(sequenceEntity.locationsCount, localSequence.compressionDetails.locationsCount)
        assertTrue(localSequence.compressionDetails is SequenceDetailsCompressionVideo)
        assertNotNull(sequenceEntity.onlineID)
        assertEquals(sequenceEntity.onlineID, localSequence.details.onlineId)
        assertNotNull(sequenceEntity.boundingNorthLat)
        assertNotNull(sequenceEntity.boundingSouthLat)
        assertNotNull(sequenceEntity.boundingWestLon)
        assertNotNull(sequenceEntity.boundingEastLon)
    }

    @Test
    fun toLocalSequenceWithReward() {
        val sequenceRewardEntity = generateSequenceWithRewardEntity(false, 5)

        val localSequence = DataConverter.toLocalSequence(scoreDataSource, sequenceRewardEntity)

        assertSequenceEntityNonNullables(sequenceRewardEntity.sequenceEntity, localSequence)
        assertNotNull(sequenceRewardEntity.scoreEntities)
        assertFalse(sequenceRewardEntity.scoreEntities.isEmpty())
    }

    @Test
    fun toLocalSequenceWithRewardWithNullableFields() {
        val sequenceRewardEntity = generateSequenceWithRewardEntity(true, 5)

        val localSequence = DataConverter.toLocalSequence(scoreDataSource, sequenceRewardEntity)

        assertSequenceEntityNonNullables(sequenceRewardEntity.sequenceEntity, localSequence)
        assertNotNull(sequenceRewardEntity.scoreEntities)
        assertTrue(sequenceRewardEntity.scoreEntities.isEmpty())
    }

    @Test
    fun toLocalSequenceWithJpegCompresion() {
        var videoCount = 0
        val sequenceEntity = generateSequenceEntity(false, videoCount)

        val localSequence = DataConverter.toLocalSequence(sequenceEntity)

        assertSequenceEntityNonNullables(sequenceEntity, localSequence)
        assertEquals(sequenceEntity.isObd, localSequence.details.isObd)
        assertNotNull(sequenceEntity.addressName)
        assertEquals(sequenceEntity.addressName, localSequence.details.addressName)
        assertNotNull(sequenceEntity.videoCount)
        assertEquals(sequenceEntity.videoCount, videoCount)
        assertEquals(sequenceEntity.locationsCount, localSequence.compressionDetails.length)
        assertEquals(sequenceEntity.locationsCount, localSequence.compressionDetails.locationsCount)
        assertTrue(localSequence.compressionDetails is SequenceDetailsCompressionJpeg)
        assertNotNull(sequenceEntity.onlineID)
        assertEquals(sequenceEntity.onlineID, localSequence.details.onlineId)
        assertNotNull(sequenceEntity.boundingNorthLat)
        assertNotNull(sequenceEntity.boundingSouthLat)
        assertNotNull(sequenceEntity.boundingWestLon)
        assertNotNull(sequenceEntity.boundingEastLon)
    }

    @Test
    fun toFrameWithLocationEntity() {
        val frameWithLocationEntity = generateFrameWIthLocationEntity(false)

        val frame = DataConverter.toFrame(frameWithLocationEntity)

        assertFrameWithNonNullables(frameWithLocationEntity.frameEntity, frame)
        assertNotNull(frameWithLocationEntity.locationEntity)
    }

    private fun assertSequenceEntityNonNullables(sequenceEntity: SequenceEntity, localSequence: LocalSequence) {
        assertEquals(sequenceEntity.sequenceId, localSequence.id)
        //assertEquals(sequenceEntity.latitude, localSequence.details.initialLocation.latitude)
        //assertEquals(sequenceEntity.longitude, localSequence.details.initialLocation.longitude)
        assertEquals(sequenceEntity.distance, localSequence.details.distance)
        assertEquals(sequenceEntity.appVersion, localSequence.details.appVersion)
        assertEquals(sequenceEntity.creationTime, localSequence.details.dateTime)
        assertEquals(sequenceEntity.locationsCount, localSequence.compressionDetails.locationsCount)
        assertEquals(sequenceEntity.diskSize, localSequence.localDetails.diskSize)
        assertEquals(sequenceEntity.filePath, localSequence.localDetails.folder.path)

    }

    private fun assertScoreEntityNonNullables(scoreEntity: ScoreEntity, scoreHistory: ScoreHistory) {
        assertEquals(scoreEntity.scoreId, scoreHistory.id)
        assertEquals(scoreEntity.coverage, scoreHistory.coverage)
    }

    private fun assertFrameWithNonNullables(frameEntity: FrameEntity, frame: Frame) {
        assertEquals(frameEntity.frameId, frame.id)
        assertEquals(frameEntity.filePath, frame.filePath)
        assertEquals(frameEntity.index, frame.index)
    }

    private fun assertLocationWithNonNullables(locationEntity: LocationEntity, location: KVLocation) {
        assertEquals(locationEntity.locationId, location.id)
        //assertEquals(locationEntity.latitude, location.location.latitude)
        //assertEquals(locationEntity.longitude, location.location.longitude)
        assertEquals(locationEntity.sequenceID, location.sequenceId)
    }

    private fun assertVideoEntityWithNonNullables(videoEntity: VideoEntity, video: Video) {
        assertEquals(videoEntity.filePath, video.path)
        assertEquals(videoEntity.frameCount, video.locationsCount)
        assertEquals(videoEntity.index, video.index)
    }

    private fun generateScoreEntity(nullableFields: Boolean): ScoreEntity {
        var obdFrameCount: Int? = null
        var frameCount: Int? = null
        val random = Random()
        if (!nullableFields) {
            obdFrameCount = random.nextInt()
            frameCount = random.nextInt()
        }
        return ScoreEntity(
                UUID.randomUUID().toString(),
                obdFrameCount,
                frameCount,
                random.nextInt(11),
                UUID.randomUUID().toString()
        )
    }

    private fun generateFrameEntity(nullableFields: Boolean): FrameEntity {
        val random = Random()
        var dateTime: DateTime? = null
        if (!nullableFields) {
            dateTime = DateTime(random.nextLong())
        }
        return FrameEntity(
                UUID.randomUUID().toString(),
                dateTime,
                UUID.randomUUID().toString(),
                random.nextInt(),
                UUID.randomUUID().toString())
    }

    private fun generateFrame(nullableFields: Boolean): Frame {
        val random = Random()
        return if (nullableFields)
            Frame(
                    UUID.randomUUID().toString(),
                    UUID.randomUUID().toString(),
                    DateTime(),
                    random.nextInt())
        else
            Frame(
                    UUID.randomUUID().toString(),
                    UUID.randomUUID().toString(),
                    DateTime(),
                    random.nextInt(),
                    Location(UUID.randomUUID().toString()))
    }

    private fun generateScoreHistory(): ScoreHistory {
        val random = Random()
        return ScoreHistory(
                UUID.randomUUID().toString(),
                random.nextInt(11),
                random.nextInt(),
                random.nextInt()
        )
    }

    private fun generateVideoEntity(): VideoEntity {
        val random = Random()
        return VideoEntity(
                UUID.randomUUID().toString(),
                random.nextInt(),
                UUID.randomUUID().toString(),
                random.nextInt(),
                UUID.randomUUID().toString())
    }

    private fun generateSequenceEntity(nullableFields: Boolean, videoCount: Int): SequenceEntity {
        val random = Random()
        return SequenceEntity(
                UUID.randomUUID().toString(),
                if (!nullableFields) random.nextBoolean() else null,
                random.nextDouble(),
                random.nextDouble(),
                if (!nullableFields) UUID.randomUUID().toString() else null,
                random.nextDouble(),
                UUID.randomUUID().toString(),
                DateTime(),
                random.nextInt(),
                videoCount,
                random.nextLong(),
                UUID.randomUUID().toString(),
                if (!nullableFields) random.nextLong() else null,
                random.nextInt(3),
                if (!nullableFields) random.nextDouble() else null,
                if (!nullableFields) random.nextDouble() else null,
                if (!nullableFields) random.nextDouble() else null,
                if (!nullableFields) random.nextDouble() else null)
    }

    private fun generateLocationEntity(nullableFields: Boolean): LocationEntity {
        val random = Random()
        var videoId: String? = null
        var frameId: String? = null
        if (!nullableFields) {
            videoId = UUID.randomUUID().toString()
            frameId = UUID.randomUUID().toString()
        }

        return LocationEntity(
                UUID.randomUUID().toString(),
                random.nextDouble(),
                random.nextDouble(),
                UUID.randomUUID().toString(),
                videoId,
                frameId
        )
    }

    private fun generateVideo(): Video {
        val random = Random()
        return Video(
                UUID.randomUUID().toString(),
                random.nextInt(),
                random.nextInt(),
                UUID.randomUUID().toString()
        )
    }

    private fun generateKvLocation(): KVLocation {
        return KVLocation(
                UUID.randomUUID().toString(),
                generalAndroidLocation(),
                UUID.randomUUID().toString())
    }

    private fun generateLocalSequence(nullableFields: Boolean, videoCompression: Boolean, paid: Boolean = false): LocalSequence {
        val random = Random()
        val sequenceID = UUID.randomUUID().toString()
        val sequenceDetails = SequenceDetails(
                generalAndroidLocation(),
                random.nextDouble(),
                UUID.randomUUID().toString(),
                DateTime())
        val sequenceDetailsLocal = SequenceDetailsLocal(
                KVFile(UUID.randomUUID().toString()),
                random.nextLong(),
                random.nextInt(3))
        val video = SequenceDetailsCompressionVideo(
                random.nextInt(),
                UUID.randomUUID().toString(),
                random.nextInt())
        val jpeg = SequenceDetailsCompressionJpeg(
                random.nextInt(),
                UUID.randomUUID().toString(),
                random.nextInt())
        return if (nullableFields)
            LocalSequence(
                    sequenceID,
                    sequenceDetails,
                    sequenceDetailsLocal,
                    if (videoCompression) video else jpeg)
        else LocalSequence(
                sequenceID,
                sequenceDetails,
                sequenceDetailsLocal,
                if (videoCompression) video else jpeg,
                SequenceDetailsRewardPoints(random.nextDouble(), UUID.randomUUID().toString(), mapOf<Int, ScoreHistory>(1 to generateScoreHistory(), 2 to generateScoreHistory(), 3 to generateScoreHistory()))
        )
    }

    private fun generateSequenceWithRewardEntity(nullableFields: Boolean, videoCount: Int): SequenceWithRewardEntity {
        val sequenceWithRewardEntity = SequenceWithRewardEntity(generateSequenceEntity(nullableFields, videoCount))
        sequenceWithRewardEntity.scoreEntities =
                if (nullableFields) listOf()
                else List(100) {
                    generateScoreEntity(nullableFields)
                }
        return sequenceWithRewardEntity
    }

    private fun generateFrameWIthLocationEntity(nullableFields: Boolean): FrameWithLocationEntity {
        val frameWithLocationEntity = FrameWithLocationEntity()
        if (!nullableFields) {
            frameWithLocationEntity.frameEntity = generateFrameEntity(nullableFields)
            frameWithLocationEntity.locationEntity = generateLocationEntity(nullableFields)
        }
        return frameWithLocationEntity
    }

    private fun generalAndroidLocation(): Location {
        val location = Location(UUID.randomUUID().toString())
        val random = Random()
        location.latitude = random.nextDouble()
        location.longitude = random.nextDouble()
        return location
    }
}