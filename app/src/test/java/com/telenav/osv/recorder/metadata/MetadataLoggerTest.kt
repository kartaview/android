package com.telenav.osv.recorder.metadata

import com.telenav.RandomUtils
import com.telenav.osv.recorder.metadata.model.MetadataHeader
import com.telenav.osv.recorder.metadata.model.body.*
import com.telenav.osv.utils.StringUtils
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.MockitoAnnotations

class MetadataLoggerTest {

    var metadataLogger: MetadataLogger? = null

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        metadataLogger = MetadataLogger()
    }

    @After
    fun tearDown() {
        metadataLogger = null
    }

    @Test
    fun headerWithBody() {
        val metadataHeaderWithBody = metadataLogger?.headerWithBody()

        assertNotNull(metadataHeaderWithBody)
        metadataHeaderWithBody?.let {
            //split based on the new line
            //removes the last \n so the split won't return an empty space
            val metadataHeaderWithBodyWithoutLastSpace = it.substring(0, it.length - 1)
            val splittedHeaderWithBody = metadataHeaderWithBodyWithoutLastSpace.split("\n")

            // the + 3 represents the header, metadata and body identifiers
            assertEquals(splittedHeaderWithBody.size, TemplateID.values().size + 3)
            //check if the first is the metadata title
            assertEquals(splittedHeaderWithBody[0], MetadataHeader.IDENTIFIER_METADATA)
            assertEquals(splittedHeaderWithBody[1], MetadataHeader.IDENTIFIER_HEADER)
            assertEquals(splittedHeaderWithBody[splittedHeaderWithBody.size - 1], MetadataHeader.IDENTIFIER_BODY)
            // skip the BODY identifier
            val templateIdValues = TemplateID.values()
            for (i in templateIdValues.indices) {
                //skip always the metadata and header identifiers
                val headerSplit = splittedHeaderWithBody[i + 2].split(MetadataHeader.DELIMITER)
                //no of function header line
                assertEquals(headerSplit.size, 4)
                assertEquals(headerSplit[0], "${MetadataHeader.IDENTIFIER_FUNCTION}:${templateIdValues[i].value}")
                assertEquals(headerSplit[1], templateIdValues[i].name)
                assertEquals(headerSplit[2], MetadataBodyBase.METADATA_VERSION.toString())
                assertEquals(headerSplit[3], MetadataBodyBase.METADATA_VERSION_COMPATIBLE_MIN.toString())
            }
        }
    }

    @Test
    fun footer() {
        val footer = metadataLogger?.footer()

        assertNotNull(footer)
        assertEquals(footer, "END")
    }

    @Test
    fun `test obd body metadata`() {
        val obdBody = MetadataBodyObd(RandomUtils.generateString(), RandomUtils.generateInt())
        val result = obdBody.toString()

        testMetadataBody(obdBody, result)
    }

    @Test
    fun testPressure() {
        val pressureBody = MetadataBodyPressure(RandomUtils.generateString(), RandomUtils.generateFloat())
        val result = pressureBody.toString()

        testMetadataBody(pressureBody, result)
    }

    @Test
    fun testCompass() {
        val compassBody = MetadataBodyCompass(RandomUtils.generateString(), RandomUtils.generateFloat())
        val result = compassBody.toString()

        testMetadataBody(compassBody, result)
    }

    @Test
    fun testAttitude() {
        val attitudeBody = MetadataBodyAttitude(RandomUtils.generateString(), RandomUtils.generateFloat(), RandomUtils.generateFloat(), RandomUtils.generateFloat())
        val result = attitudeBody.toString()

        testMetadataBody(attitudeBody, result)
    }

    @Test
    fun testAcceleration() {
        val accelerationBody = MetadataBodyAcceleration(RandomUtils.generateString(), RandomUtils.generateFloat(), RandomUtils.generateFloat(), RandomUtils.generateFloat())
        val result = accelerationBody.toString()

        testMetadataBody(accelerationBody, result)
    }

    @Test
    fun testGravity() {
        val gravityBody = MetadataBodyGravity(RandomUtils.generateString(), RandomUtils.generateFloat(), RandomUtils.generateFloat(), RandomUtils.generateFloat())
        val result = gravityBody.toString()

        testMetadataBody(gravityBody, result)
    }

    @Test
    fun testExif() {
        val exifBody = MetadataBodyCameraExif(RandomUtils.generateString(), RandomUtils.generateFloat(), RandomUtils.generateInt(), RandomUtils.generateInt())
        val result = exifBody.toString()

        testMetadataBody(exifBody, result)
    }

    @Test
    fun testCamera() {
        val cameraBody = MetadataBodyCamera(RandomUtils.generateString(), RandomUtils.generateDouble(), RandomUtils.generateDouble(), RandomUtils.generateFloat())
        val result = cameraBody.toString()

        testMetadataBody(cameraBody, result)
    }

    @Test
    fun testPhotoVideo() {
        val photoVideoBody = MetadataPhotoVideo(timeStamp = RandomUtils.generateString(), frameIndex = RandomUtils.generateInt(), videoIndex = RandomUtils.generateInt(),
                gpsTimestamp = RandomUtils.generateString(), latitude = RandomUtils.generateDouble(), longitude = RandomUtils.generateDouble(),
                horizonAccuracy = RandomUtils.generateFloat(), gpsSpeed = RandomUtils.generateFloat(), compassTimeStamp = null, compass = null, obdTimestamp = null, obd2Speed = null)
        val result = photoVideoBody.toString()

        testMetadataBody(photoVideoBody, result)
    }

    @Test
    fun testGps() {
        val gpsBody = MetadataBodyGps(RandomUtils.generateString(), RandomUtils.generateDouble(), RandomUtils.generateDouble(), RandomUtils.generateDouble(), RandomUtils.generateFloat(), RandomUtils.generateFloat(), RandomUtils.generateFloat())
        val result = gpsBody.toString()

        testMetadataBody(gpsBody, result)
    }

    @Test
    fun testDevice() {
        val deviceBody = MetadataBodyDevice(RandomUtils.generateString(), RandomUtils.generateString(), RandomUtils.generateString(), RandomUtils.generateString(), RandomUtils.generateString(), RandomUtils.generateString(), RandomUtils.generateString(), RandomUtils.generateString())
        val result = deviceBody.toString()

        testMetadataBody(deviceBody, result)
    }

    @Test
    fun testNoTimestampBody(){
        val obdBodyNoTimestamp = MetadataBodyObd("", RandomUtils.generateInt())

        assertEquals(obdBodyNoTimestamp.toString(), StringUtils.EMPTY_STRING)
    }

    private fun testMetadataBody(metadataBodyBase: MetadataBodyBase, result: String) {
        assertNotNull(result)

        val splittedByColon = result.split(MetadataBodyBase.COLON)
        //test timestamp
        val timestamp = splittedByColon[0]
        assertNotNull(timestamp)
        assertNotEquals(timestamp, StringUtils.EMPTY_STRING)
        //test template
        val template = splittedByColon[1]
        assertNotNull(template)
        assertEquals(template, metadataBodyBase.templateId.value)

        val spilltedFields = splittedByColon[2].split(MetadataBodyBase.DELIMITER)
        val fieldsSize = metadataBodyBase.fields.size
        assertEquals(spilltedFields.size, fieldsSize)

        //test fields
        for (i in metadataBodyBase.fields.indices) {
            var splittedField = spilltedFields[i]
            if (i == fieldsSize - 1) {
                //remove the \n at the end
                splittedField = splittedField.substring(0, splittedField.length - 1)
            }
            assertEquals(metadataBodyBase.fields[i]?.toString() ?: StringUtils.EMPTY_STRING, splittedField)
        }
    }
}