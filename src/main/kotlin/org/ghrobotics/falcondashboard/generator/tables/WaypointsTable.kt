package org.ghrobotics.falcondashboard.generator.tables

import edu.wpi.first.wpilibj.geometry.Pose2d
import edu.wpi.first.wpilibj.geometry.Rotation2d
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.input.ClipboardContent
import javafx.scene.input.DataFormat
import javafx.scene.input.TransferMode
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.util.converter.DoubleStringConverter
import org.ghrobotics.falcondashboard.generator.GeneratorView
import org.ghrobotics.falcondashboard.generator.tables.WaypointsTable.setRowFactory
import org.ghrobotics.lib.mathematics.twodim.geometry.Pose2d
import org.ghrobotics.lib.mathematics.twodim.geometry.Translation2d
import org.ghrobotics.lib.mathematics.twodim.geometry.x_u
import org.ghrobotics.lib.mathematics.twodim.geometry.y_u
import org.ghrobotics.lib.mathematics.units.derived.degrees
import org.ghrobotics.lib.mathematics.units.inMeters
import org.ghrobotics.lib.mathematics.units.meters
import org.ghrobotics.falcondashboard.Settings.rotateWaypoints
import tornadofx.column
import tornadofx.point
import java.io.File
import java.io.IOException
import kotlin.math.round
import kotlin.math.cos
import kotlin.math.sin


object WaypointsTable : TableView<Pose2d>(GeneratorView.waypoints) {

    private val columnX = column<Pose2d, Double>("X") {
        SimpleObjectProperty(round(it.value.translation.x_u.inMeters() * 1E3) / 1E3)
    }

    private val columnY = column<Pose2d, Double>("Y") {
        SimpleObjectProperty(round(it.value.translation.y_u.inMeters() * 1E3) / 1E3)
    }

    private val columnAngle = column<Pose2d, Double>("Angle") {
        SimpleObjectProperty(round(it.value.rotation.degrees * 1E3) / 1E3)
    }

    private val cellFactory = {
        val cell = TextFieldTableCell<Pose2d, Double>()
        cell.converter = DoubleStringConverter()
        cell
    }

    init {
        isEditable = true

        columnResizePolicy = CONSTRAINED_RESIZE_POLICY

        columns.forEach {
            it.isSortable = false
            it.isReorderable = false
        }

        with(columnX) {
            setCellFactory { cellFactory() }
            setOnEditCommit {
                val history = it.rowValue
                this@WaypointsTable.items[it.tablePosition.row] = Pose2d(
                    Translation2d(it.newValue.meters, history.translation.y_u),
                    history.rotation
                )
                this@WaypointsTable.refresh()
            }
        }
        with(columnY) {
            setCellFactory { cellFactory() }
            setOnEditCommit {
                val history = it.rowValue
                this@WaypointsTable.items[it.tablePosition.row] = Pose2d(
                    Translation2d(history.translation.x_u, it.newValue.meters),
                    history.rotation
                )
                this@WaypointsTable.refresh()
            }
        }
        with(columnAngle) {
            setCellFactory { cellFactory() }
            setOnEditCommit {
                val history = it.rowValue
                this@WaypointsTable.items[it.tablePosition.row] = Pose2d(
                    history.translation,
                    Rotation2d.fromDegrees(it.newValue)
                )
                this@WaypointsTable.refresh()
            }
        }

        setRowFactory { _ ->
            val row = TableRow<Pose2d>()

            row.setOnDragDetected {
                if (!row.isEmpty) {
                    val index = row.index
                    val db = startDragAndDrop(TransferMode.MOVE)
                    db.dragView = row.snapshot(null, null)

                    val cc = ClipboardContent()
                    cc.putString(index.toString())
                    db.setContent(cc)
                    it.consume()
                }
            }

            row.setOnDragOver {
                if (it.dragboard.hasString()) {
                    if (row.index != it.dragboard.getContent(DataFormat.PLAIN_TEXT).toString().toInt()) {
                        it.acceptTransferModes(*TransferMode.COPY_OR_MOVE)
                        it.consume()
                    }
                }
                it.consume()
            }

            row.setOnDragDropped {
                val db = it.dragboard
                if (db.hasString()) {
                    val dragIndex = db.getContent(DataFormat.PLAIN_TEXT).toString().toInt()
                    val dropIndex = if (row.isEmpty) {
                        this@WaypointsTable.items.size
                    } else row.index

                    if (this@WaypointsTable.items.size > 2) {
                        this@WaypointsTable.items.add(dropIndex, this@WaypointsTable.items.removeAt(dragIndex))
                    } else {
                        this@WaypointsTable.items.reverse()
                    }
                    it.isDropCompleted = true
                    it.consume()
                }
            }
            return@setRowFactory row
        }
    }

    fun getRotatedPoint(pointToRotate: Pose2d, origin: Pose2d): Pose2d
    {
        // Assuming counterclockwise
        var rotateBy = -Math.toRadians(origin.rotation.degrees)
        var xdiff = pointToRotate.translation.x - origin.translation.x
        var ydiff = pointToRotate.translation.y - origin.translation.y
        var newx = (xdiff * cos(rotateBy) - ydiff * sin(rotateBy)) + origin.translation.x
        var newy = (xdiff * sin(rotateBy) + ydiff * cos(rotateBy)) + origin.translation.y
        var angle = pointToRotate.rotation.degrees - origin.rotation.degrees
        if (angle > 180) {
            angle -= 360
        }
        else if (angle < -180) {
            angle += 360
        }
        return Pose2d(Translation2d(newx.meters, newy.meters), Rotation2d(Math.toRadians(angle)))
    }

    fun zeroTranslation(point: Pose2d, origin: Pose2d): Pose2d
    {
        var x = point.translation.x - origin.translation.x
        var y = point.translation.y - origin.translation.y
        return Pose2d(Translation2d(x.meters, y.meters), Rotation2d(point.rotation.radians))
    }

    fun getWriteStrFromPose(pose: Pose2d): String
    {
        var x = pose.translation.x.toString()
        var y = pose.translation.y.toString()
        var angle = pose.rotation.degrees.toString()
        var writeStr = x + "," + y + "," + angle
        return writeStr
    }

    fun saveToFile()
    {
        try
        {
            val dir = File("Paths/")
            dir.mkdirs()
            // Create file chooser
            val fileChooser = FileChooser()
            fileChooser.setInitialDirectory(dir)
            fileChooser.title = "Save File" //set the title of the Dialog window
            val defaultSaveName = "path.txt"
            fileChooser.initialFileName = defaultSaveName //set the default name for file to be saved
            //create extension filters. The choice will be appended to the end of the file name
            fileChooser.extensionFilters.addAll(
                FileChooser.ExtensionFilter("Text Files", "*.txt")
            )
            // Open window
            val stg = Stage()
            // Get filename
            val file = fileChooser.showSaveDialog(stg)
            // Close the window
            stg.close()
            file.printWriter().use { out ->
                out.println("X (m),Y (m),Angle (deg)")
                var origin = GeneratorView.waypoints.get(0)
                out.println(getWriteStrFromPose(Pose2d(Translation2d(0.meters, 0.meters), Rotation2d(0.0))))
                // out.println(getWriteStrFromPose(Pose2d(Translation2d(origin.translation.x.meters, origin.translation.y.meters), Rotation2d(0.0))))
                for (idx in 1 until GeneratorView.waypoints.size)
                {
                    if(rotateWaypoints.value) {
                        var point = zeroTranslation(getRotatedPoint(GeneratorView.waypoints.get(idx), origin), origin)
                        out.println(getWriteStrFromPose(point))
                    }
                    else {
                        out.println(getWriteStrFromPose(GeneratorView.waypoints.get(idx)))
                    }
                }
            }
        }
        catch (e: IOException) {
            print(e)
        }
    }

    fun loadFromFile()
    {
        val dir = File("Paths/")
        dir.mkdirs()
        // Create poses
        val poses= mutableListOf<Pose2d?>()
        // Create FileChooser
        val fileChooser = FileChooser()
        fileChooser.setInitialDirectory(dir)
        fileChooser.title = "Load File" //set the title of the Dialog window
        //create extension filters. The choice will be appended to the end of the file name
        fileChooser.extensionFilters.addAll(
            FileChooser.ExtensionFilter("Text Files", "*.txt")
        )
        // Open window
        val stg = Stage()
        // Get the file
        val file = fileChooser.showOpenDialog(stg)
        // Close the window
        stg.close()
        val lines = file.readLines()
        // Loop over the file ignoring the first line (head)
        for (idx in 1 until lines.size)
        {
            val line = lines.get(idx)
            val words = line.split(",")
            val pose = Pose2d(Translation2d(words[0].toDouble().meters, words[1].toDouble().meters), Rotation2d(Math.toRadians(words[2].toDouble())))
            poses.add(pose)
        }
        // Set Waypoints
        GeneratorView.waypoints.setAll(poses)

    }

    fun loadFromText(text: String) {
        val lines = text.lines()

        val poses: List<Pose2d?> = lines.map {
            if(it.isEmpty()) return@map null
            var trim = it
                .replace(" ", "")
                .let { it2 -> if(it2.last() == ',') it2.substring(0, it2.length - 1) else it2 }
                .let { it2 -> if(!it2.startsWith("Pose2d", true)) null else it2 } ?: return@map null

            // so at this point all of our text starts with Pose2d and ends with a closing paren.
            // start by removing the starting and closing parenthesis

            trim = trim.substring(7, trim.length- 1)
            val x = trim.substring(0, trim.indexOf(".meters"))
                .let { it2 ->
                    if(it2.startsWith("(") || it2.endsWith(")")) {
                        return@let it2.substring(1, it2.length - 1)
                    } else it2
                }
                .toDouble()
            val trimNoX = trim.substring(trim.indexOf(".meters") + 6, trim.length)
            val y = trimNoX.substring(0, trimNoX.indexOf(".meters"))
                .let { it2 ->
                    if(it2.startsWith("(") || it2.endsWith(")")) {
                        return@let it2.substring(1, it2.length - 1)
                    } else it2
                }
                .toDouble()
            val trimNoY = trimNoX.substring(trimNoX.indexOf(".meters") + 6, trimNoX.length)
            val theta: Double = trimNoY.let { noY ->
                val index: Int = noY.indexOf(".degrees").let { ret ->
                    if(ret < 0) noY.indexOf(".degree") else ret
                }
                val numberWithMaybeParens = noY.substring(0, index)
                if(numberWithMaybeParens.startsWith("(") || numberWithMaybeParens.endsWith(")")) {
                    return@let numberWithMaybeParens.substring(1, numberWithMaybeParens.length - 1).toDouble()
                }
                return@let numberWithMaybeParens.toDouble()
            }
            val pose = Pose2d(x.meters, y.meters, theta.degrees)
            pose
        }
        GeneratorView.waypoints.setAll(poses.filterNotNull())
    }

    fun removeSelectedItemIfPossible() {
        val item = selectionModel.selectedItem
        if (item != null && items.size > 2) GeneratorView.waypoints.remove(item)
    }
}
