package dev.nezhar.lookabout

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** The folder, under the phone's shared storage, that walk photos are copied to. */
const val GALLERY_FOLDER = "Pictures/Lookabout"

private val galleryStamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

/** The gallery file name of a photo taken at [time], such as `lookabout-20261011-143005.jpg`. */
fun galleryName(time: LocalDateTime): String = "lookabout-${time.format(galleryStamp)}.jpg"
