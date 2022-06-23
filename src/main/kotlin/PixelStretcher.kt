import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import java.awt.Color
import java.awt.Transparency
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

    private val type by option().choice("down", "fade").default("down")

    override fun run() {

        val input = inputPath.immutableImage()

        when (type) {
            "down" -> {
                val lastColor = arrayOfNulls<Color>(input.width)

                val totalPixels = input.pixels().size.toDouble()
                var count = 0
                input.map { pixel ->
                    println(++count / totalPixels)
                    when {
                        pixel.alpha() == 255 -> pixel.toColor().awt().also { lastColor[pixel.x] = it }
                        else -> lastColor[pixel.x] ?: pixel.toColor().awt()
                    }
                }.save()
            }
            "fade" -> {
                val fade = Array<Array<Color?>>(input.width) { arrayOfNulls(input.height) }

                (0 until input.width).forEach { x->
                    input.col(x).asSequence().withIndex()
                        .filter { it.value.toColor().awt().transparency == Transparency.OPAQUE }
                        .zipWithNext { (indexA, pixelA), (indexB, pixelB) ->
                            if (indexA + 1 != indexB) {
                                val range = (indexA + 1) until indexB
                                range.zip(
                                        fadeColor(
                                            start = pixelA.toColor().awt(),
                                            end = pixelB.toColor().awt(),
                                            steps = range.count(),
                                        )
                                    )
                            } else {
                                emptyList()
                            }
                        }
                        .flatten()
                        .forEach { (y, color) ->  fade[x][y] = color }
                }

                val totalPixels = input.pixels().size.toDouble()
                var count = 0
                input.map { pixel ->
                    println(++count / totalPixels)
                    when {
                        pixel.alpha() == 255 -> pixel.toColor().awt()
                        else ->  fade[pixel.x][pixel.y] ?: pixel.toColor().awt()
                    }
                }.save()
            }
            else -> error("type = $type")
        }
    }

    private fun ImmutableImage.save(): File? {
        val filename = "${LocalDateTime.now().format(DATE_FORMAT)}.png"
        echo(message = "Filename: $filename")
        return output(PngWriter.MinCompression, inputPath.resolveSibling(relative = filename))
    }
}

fun main(argv: Array<String>) = PixelStretcher.main(argv)

private fun File.immutableImage() = ImmutableImage.loader().fromFile(this)

private fun fadeColor(start: Color, end: Color, steps: Int): List<Color> {
    require(steps > 0)
    return (0 until steps)
        .map { it.toDouble() + 0.5 }
        .map { it / steps }
        .map { fraction ->
            Color(
               (fraction * end.red + (1 - fraction) * start.red).toInt(),
               (fraction * end.green + (1 - fraction) * start.green).toInt(),
               (fraction * end.blue + (1 - fraction) * start.blue).toInt(),
            )
        }
}
