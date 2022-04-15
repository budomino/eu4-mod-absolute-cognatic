import okio.Path
import okio.Path.Companion.toPath
import okio.FileSystem.Companion.SYSTEM as System
import okio.buffer
import okio.use
import kotlin.math.absoluteValue

fun main(args: Array<String>) {
	val directory =
		if (args.isNotEmpty()){
			args[0].toPath()
		} else {
			"".toPath()
		}
	val modRootDirectory = System.list(directory)
	if (modRootDirectory.isEmpty())
		println("Directory appears to be empty. Will not continue.")
	else {
		val acAndRoman = listOf<String>()
		val justAC = listOf<String>()
		for (file in System.list(("$modRootDirectory/history/countries").toPath())) {
			if (file.isTextFile()){
				var religion: String? = null
				var culture: String? = null
				System.source(file).use { fileSource ->
					fileSource.buffer().use { bufferSource ->
						while (true){
							val line = bufferSource.readUtf8Line() ?: break
							if (line.contains("religion"))
								religion = line.substring(line.indexOf("=") + 1).trim()
							else if (line.contains("primary_culture"))
								culture = line.substring(line.indexOf("=") + 1).trim()
							if ((religion != null) && (culture != null)) break
						}
					}
				}
				if ((religion != null) && (culture != null)){
					val isReformist = (religion == "converted_dynamic_faith_102")
					val isRoman = (culture == "dynamic-greek-sicilian-culture-num1")
					
				}
			}
		}
		for (file in System.list(directory)) {
			if (file.name.contains(".txt")) {
				//val fullFile = mutableListOf<String>()
				System.source(file).use { fileSource ->
					fileSource.buffer().use { bufferedSource ->
						var monarchBlock = false
						var femaleChance = 0
						var maleChance = 0
						while (true) {
							val line = bufferedSource.readUtf8Line() ?: break
							if (line.contains("monarch_names = {")) monarchBlock = true
							else if (monarchBlock && line.contains("}")) monarchBlock = false
							if (monarchBlock && line.contains("=") && !line.contains("{") && !line.contains("}")) {
								val value = line.substring(line.indexOf("=") + 1).trim()
								try {
									if (value.contains("-")) {
										femaleChance -= value.toInt()
									} else {
										maleChance += value.toInt()
									}
								} catch (e: NumberFormatException) {
									println("cannot parse: $value")
								}
							}
							//	fullFile.add(line)
						}
						if ((femaleChance != 0) && (maleChance != 0)) {
							femaleChance = femaleChance.absoluteValue
							maleChance = maleChance.absoluteValue
							val totalChance = (femaleChance + maleChance).toDouble()
							val fPercentageString = ((femaleChance / totalChance) * 100).toString()
							val fPercentage = fPercentageString.substring(0, substringRoundDown(fPercentageString))
							val mPercentageString = ((maleChance / totalChance) * 100).toString()
							val mPercentage = mPercentageString.substring(0, substringRoundDown(mPercentageString))
							println(
								"[${
									file.name.removeSuffix(".txt").uppercase()
								}] Female: $femaleChance/${totalChance.toInt()} ($fPercentage%); Male: $maleChance/${totalChance.toInt()} ($mPercentage%)"
							)
						} else if ((femaleChance != 0) && (maleChance == 0))
							println(
								"[${
									file.name.removeSuffix(".txt").uppercase()
								}] Completely Female"
							)
						else if ((femaleChance == 0) && (maleChance != 0))
							println(
								"[${
									file.name.removeSuffix(".txt").uppercase()
								}] Completely Male"
							)
						else
							println(
								"[${
									file.name.removeSuffix(".txt").uppercase()
								}] Unexpected Result. Unknown Ratio."
							)
					}
				}
//			val outputPath = "/tmp/output/" + file.name
//			System.write(outputPath.toPath()){
//				this.writeUtf8(fullFile.)
//			}
			}
		}
	}
}

fun substringRoundDown(string: String): Int {
	val periodIndex = string.indexOf(".")
	return when(periodIndex - string.lastIndex) {
		-1 -> periodIndex
		0 -> periodIndex
		1 -> periodIndex
		2 -> periodIndex + 1
		3 -> periodIndex + 2
		else -> periodIndex + 3
	}
}

fun Path.isTextFile(): Boolean {
	return this.name.contains(".txt")
}