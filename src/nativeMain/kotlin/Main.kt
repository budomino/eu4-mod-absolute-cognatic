import okio.FileSystem

fun main(args: Array<String>) {
    val directory =
    if (args.isNotEmpty()){
        args[0]
    } else {
        System.getProperty("user.dir")
    }
    val fileList = FileSystem(directory).list()
    println("$fileList")
}