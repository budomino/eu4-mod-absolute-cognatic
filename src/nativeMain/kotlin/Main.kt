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
		val acAndRoman = mutableListOf<String>()
		val justAC = mutableListOf<String>()
		val listOfFilesToOutput = mutableMapOf<String,List<String>>()
		val outputPath = "/tmp/output/"
		// retrieve the countries that have the matching religion and/or culture during the start date
		for (file in System.list("$directory/history/countries".toPath())) {
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
					val countryNameFile = file.name.substring(file.name.indexOf("-")+1).trim()
					val countryNameLog = countryNameFile.removeSuffix(".txt")
					when {
						isReformist && isRoman -> {
							println("[$countryNameLog] will be given absolute cognatic succession and Roman names")
							acAndRoman.add(countryNameFile)
						}
						!isReformist && isRoman -> {
							println("[$countryNameLog] will be given absolute cognatic succession and Roman names")
							acAndRoman.add(countryNameFile)
						}
						isReformist && !isRoman -> {
							println("[$countryNameLog] will be given absolute cognatic succession but NOT Roman names")
							justAC.add(countryNameFile)
						}
						!isReformist && !isRoman -> {
							//println("[$countryNameLog] will be left alone as it is not suited for conversion")
						}
					}
				}
			}
		}
		// retrieve the files of the countries that were identified in the previous for loop
		for (file in System.list("$directory/common/countries".toPath())) {
			if (file.isTextFile() && ((acAndRoman.contains(file.name))||(justAC.contains(file.name)))) {
				val fullFile = mutableListOf<String>()
				var femaleChance = 0
				var maleChance = 0
				var femalePercentage = 0.0
				System.source(file).use { fileSource ->
					fileSource.buffer().use { bufferedSource ->
						var monarchBlock = false
						while (true) {
							val line = bufferedSource.readUtf8Line() ?: break
							if (line.contains("monarch_names = {")) monarchBlock = true
							else if (monarchBlock && line.contains("}")) monarchBlock = false
							if (monarchBlock && line.contains("=") && !line.contains("{") && !line.contains("}")) {
								val value = line.substring(line.indexOf("=") + 1).trim()
								try {
									if (value.contains("-")) {
										femaleChance += value.toInt()
									} else {
										maleChance += value.toInt()
									}
								} catch (e: NumberFormatException) {
									println("cannot parse: $value")
								}
							}
							fullFile.add(line)
						}
					}
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
					femalePercentage = fPercentage.toDouble()
				} else if ((femaleChance != 0) && (maleChance == 0)) {
					println(
						"[${
							file.name.removeSuffix(".txt").uppercase()
						}] Completely Female"
					)
					femalePercentage = 100.0
				}
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
//				if (acAndRoman.contains(file.name)){
//
//				} else {
					if (femalePercentage < 50) {
						listOfFilesToOutput[file.name] = (modifyFemalePercentage(femalePercentage,50,fullFile,true,file.name))
					} else if (femalePercentage > 55) {
						listOfFilesToOutput[file.name] = (modifyFemalePercentage(femalePercentage,55,fullFile,false,file.name))
					} else if (femalePercentage > 50 && femalePercentage < 55){
						listOfFilesToOutput[file.name] = fullFile
					}
//				}
			}
		}
		println("Exporting files...")
		for (file in listOfFilesToOutput){
			val filePath = (outputPath + file.key).toPath()
			System.write(filePath){
				writeUtf8(file.value.joinToString("\n"))
			}
			println("$filePath")
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

fun modifyFemalePercentage(initialPercentage: Double, targetPercentage: Int, fileInListForm: List<String>, addOrSubtract: Boolean, nameOfFile: String): List<String>{
	val listToReturn = fileInListForm as MutableList<String>
	var currentPercentage = initialPercentage
	while (if (addOrSubtract)(currentPercentage < targetPercentage) else (currentPercentage > targetPercentage)) {
		var monarchBlock = false
		for ((index,line) in listToReturn.withIndex()) {
			//println("line: $line")
			if (line.contains("monarch_names = {")) monarchBlock = true
			else if (monarchBlock && line.contains("}")) monarchBlock = false
			if (monarchBlock && line.contains("=") &&
				!line.contains("{") && !line.contains("}") && line.contains("-"))
			{
				val value = line.substring(line.indexOf("=") + 1).trim().toInt()
				val newValue =
					if (addOrSubtract) {
						value - 1
					} else {
						if (value < 0)
							value + 1
						else
							value
					}
				listToReturn[index] = line.substring(0,line.indexOf("=") + 1) + " " + newValue
				//println("New Value: ${listToReturn[index]}")
			}
			currentPercentage = getCurrentRatio(listToReturn)
			//println("Current percentage for [$nameOfFile]: $currentPercentage")
			if (
				if (addOrSubtract) (currentPercentage >= targetPercentage)
				else (currentPercentage <= targetPercentage)
			)
			{
				break
			}
		}
	}
	println ("New Percentage of [$nameOfFile]: $currentPercentage")
	return listToReturn
}

fun getCurrentRatio(theListAsItStandsNow: List<String>): Double {
	var monarchBlock = false
	var femaleChance = 0
	var maleChance = 0
	for (line in theListAsItStandsNow) {
		if (line.contains("monarch_names = {")) monarchBlock = true
		else if (monarchBlock && line.contains("}")) monarchBlock = false
		if (monarchBlock && line.contains("=") &&
			!line.contains("{") && !line.contains("}"))
		{
			val value = line.substring(line.indexOf("=") + 1).trim()
			if (value.contains("-")) {
				femaleChance += value.toInt()
			} else {
				maleChance += value.toInt()
			}
		}
	}
	femaleChance = femaleChance.absoluteValue
	val totalChance = (femaleChance + maleChance.absoluteValue).toDouble()
	return ((femaleChance / totalChance)*100)
}