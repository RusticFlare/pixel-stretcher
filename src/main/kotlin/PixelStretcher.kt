import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")

object PixelStretcher : CliktCommand(
    printHelpOnEmptyArgs = true,
) {

    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    private val inputPath by argument().file(mustExist = true, canBeDir = false)

    override fun run() {

        val input = inputPath.immutableImage()

        val xyToColor = (0 until input.width)
            .map { x ->
                (sequenceOf(input.height) + (0 until input.height).reversed()
                    .asSequence()
                    .filter {y -> input.color(x,y).alpha == 255  })
                    .zipWithNext { y1,y2 -> if (y1 - 1 != y2) {
                        ((y1 - 1) downTo (y2 + 1)) to input.color(x,y2)
                    } else {null}}
                    .filterNotNull()
                    .toList()
            }

//        val coordColor = (0 until input.width).map { x -> xyToY[x]?.let { y -> input.color(x, y) } }

        val totalPixels = input.pixels().size
        var count = 0
        input.map { pixel ->
            println("${++count} of $totalPixels")
            when {
                pixel.alpha() == 255 -> pixel.toColor().awt()
                else -> xyToColor[pixel.x].firstOrNull { pixel.y in it.first }?.second?.awt() ?: pixel.toColor().awt()
            }
        }.save()
    }

    private fun ImmutableImage.yToStretch(x:Int, yStart:Int):Int? = (yStart until height)
        .reversed()
        .firstOrNull { y -> color(x, y).alpha == 255 }
//                .shuffled().first()

    private fun ImmutableImage.save(): File? {
        val filename = "${LocalDateTime.now().format(DATE_FORMAT)}.png"
        echo(message = "Filename: $filename")
        return output(PngWriter.MinCompression, inputPath.resolveSibling(relative = filename))
    }
}

fun main(argv: Array<String>) = PixelStretcher.main(argv)

private fun File.immutableImage() = ImmutableImage.loader().fromFile(this)

