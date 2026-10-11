package dev.nezhar.lookabout

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class GalleryNameTest {
    @Test
    fun nameHasPrefixPaddedTimestampAndJpgExtension() {
        assertEquals("lookabout-20261011-143005.jpg", galleryName(LocalDateTime.of(2026, 10, 11, 14, 30, 5)))
        assertEquals("lookabout-20260102-030405.jpg", galleryName(LocalDateTime.of(2026, 1, 2, 3, 4, 5)))
    }

    @Test
    fun folderIsLookaboutUnderPictures() {
        assertEquals("Pictures/Lookabout", GALLERY_FOLDER)
    }
}
