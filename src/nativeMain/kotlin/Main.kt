import okio.Path.Companion.toPath

fun main(args: Array<String>) {
	val directory =
		if (args.isNotEmpty()){
			args[0]
		} else {
		}
	println(okio.FileSystem.SYSTEM.list("".toPath()))
}