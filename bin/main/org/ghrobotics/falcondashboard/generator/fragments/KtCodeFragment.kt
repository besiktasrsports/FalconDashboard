package org.ghrobotics.falcondashboard.generator.fragments

import edu.wpi.first.wpilibj.geometry.Pose2d
import edu.wpi.first.wpilibj.geometry.Rotation2d
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import javafx.collections.ObservableList
import kfoenix.jfxtextarea
import org.ghrobotics.falcondashboard.Settings.reversed
import org.ghrobotics.falcondashboard.Settings.rotateWaypoints
import org.ghrobotics.falcondashboard.generator.GeneratorView
import org.ghrobotics.lib.mathematics.twodim.geometry.x_u
import org.ghrobotics.lib.mathematics.twodim.geometry.y_u
import org.ghrobotics.lib.mathematics.units.meters
import org.ghrobotics.lib.mathematics.units.inMeters
import org.ghrobotics.lib.mathematics.units.meter
import org.ghrobotics.lib.mathematics.twodim.geometry.Pose2d
import org.ghrobotics.lib.mathematics.twodim.geometry.Translation2d
import tornadofx.*
import java.awt.Desktop
import java.net.URI
import java.text.DecimalFormat
import kotlin.math.cos
import kotlin.math.sin


class KtCodeFragment : Fragment() {

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

    override val root = vbox {

        title = "Generated Code"

        style {
            padding = box(1.em)
        }

        prefWidth = 800.0
        prefHeight = 500.0

        jfxtextarea {
            font = Font.font("Monospaced")
            isEditable = false

            vgrow = Priority.ALWAYS

            text = buildString {

//                append(
//                    "import org.ghrobotics.lib.mathematics.twodim.geometry.Pose2d\n" +
//                            "import org.ghrobotics.lib.mathematics.twodim.trajectory.DefaultTrajectoryGenerator\n" +
//                            "import org.ghrobotics.lib.mathematics.twodim.trajectory.constraints.CentripetalAccelerationConstraint\n" +
//                            "import org.ghrobotics.lib.mathematics.units.degree\n" +
//                            "import org.ghrobotics.lib.mathematics.units.derivedunits.acceleration\n" +
//                            "import org.ghrobotics.lib.mathematics.units.derivedunits.velocity\n" +
//                            "import org.ghrobotics.lib.mathematics.units.meters\n\n\n\n"
//                )

                /*
                val name = Settings.name.value.decapitalize()
                    .replace("\\s+".toRegex(), "")
                */

                val dm = DecimalFormat("##.###")

//                append("val $name = DefaultTrajectoryGenerator.generateTrajectory(\n")
                append("Trajectory name = TrajectoryGenerator.generateTrajectory(\nList.of(\n")
                if(rotateWaypoints.value)
                {
                    var origin = GeneratorView.waypoints[0]
                    append(
                        "    new Pose2d(0, " +
                                "0, new Rotation2d(" +
                                "0))"
                    )
                    append(",")
                    append("\n")
                    for (idx in 1 until GeneratorView.waypoints.size) 
                    {
                        var point = zeroTranslation(getRotatedPoint(GeneratorView.waypoints.get(idx), origin), origin)
                        append(
                            "    new Pose2d(${dm.format(point.translation.x_u.inMeters())}, " +
                                    "${dm.format(point.translation.y_u.inMeters())}, new Rotation2d(" +
                                    "${dm.format(point.rotation.radians)}))"
                        )
                        if (idx != GeneratorView.waypoints.size-1) append(",")
                        append("\n")
                    }
                }
                else
                {
                    GeneratorView.waypoints.forEach {
                        append(
                            "    new Pose2d(${dm.format(it.translation.x_u.inMeters())}, " +
                                    "${dm.format(it.translation.y_u.inMeters())}, new Rotation2d(" +
                                    "${dm.format(it.rotation.radians)}))"
                        )
                        if (it != GeneratorView.waypoints.last()) append(",")
                        append("\n")
                    }
                }
                append("),${if (reversed.value)  "configReversed" else "configForward"});\n")

//                append(
//                    "    constraints = listOf(CentripetalAccelerationConstraint(${Settings.maxCentripetalAcceleration.value}.meters.acceleration),\n" +
//                            "    startVelocity = 0.0.meters.velocity,\n" +
//                            "    endVelocity = 0.0.meters.velocity,\n" +
//                            "    maxVelocity = ${Settings.maxVelocity.value}.meters.velocity,\n" +
//                            "    maxAcceleration = ${Settings.maxAcceleration.value}.meters.acceleration,\n" +
//                            "    reversed = ${Settings.reversed.value}\n)"
//                )
            }
        }
        vbox {
            style {
                padding = box(0.5.em, 0.em, 0.em, 0.em)
            }
            add(text(" This code is generated to be used with FalconLibrary"))
            add(hyperlink("https://github.com/5190GreenHopeRobotics/FalconLibrary") {
                setOnAction {
                    Desktop.getDesktop()
                        .browse(URI("https://github.com/5190GreenHopeRobotics/FalconLibrary"))
                }
            })
        }
    }
}