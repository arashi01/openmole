package org.openmole.gui.client.core.files

import org.openmole.gui.ext.data._

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import autowire._
import org.openmole.gui.ext.tool.client.Utils._

import scala.concurrent.duration._
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import org.openmole.gui.ext.api.Api
import org.scalajs.dom.raw.{Event, HTMLElement}
import scalatags.JsDom.all.{raw, _}
import scalatags.JsDom.TypedTag
import org.openmole.gui.ext.tool.client._
import org.openmole.gui.client.core._
import org.openmole.gui.ext.tool.client.FileManager
import DataUtils._
import net.scalapro.sortable._
import org.openmole.gui.client.core.files.TreeNodeTab.{ClosureFilter, EditableView, First100, IndexedAxis, Raw, RowFilter}
import org.openmole.gui.client.tool.{OMTags, plot}
import org.openmole.gui.client.tool.plot.Plot._
import org.openmole.gui.client.tool.plot._
import scaladget.bootstrapnative.{DataTable, ToggleButton}
import org.openmole.gui.ext.tool._
import rx._

import scala.collection.immutable.HashMap
import scala.scalajs.js.timers._

object TreeNodeTabs {

  sealed trait Activity

  object Active extends Activity

  object UnActive extends Activity

  val omsErrorCache =
  //collection.mutable.HashMap[SafePath, Seq[(ErrorWithLocation, String)]]()
    Var(HashMap[SafePath, EditorErrors]())

  //  def cache(sp: SafePath, editorErrors: EditorErrors) = {
  //    omsErrorCache.update(omsErrorCache.now.updated(sp, editorErrors))
  //  }

  def errors(safePath: SafePath)(implicit ctx: Ctx.Owner): Rx[Seq[ErrorFromCompiler]] = omsErrorCache.map {
    _.get(safePath).map { ee ⇒ ee.errorsFromCompiler }.getOrElse(Seq())
  }

  def errorsInEditor(safePath: SafePath)(implicit ctx: Ctx.Owner): Rx[Seq[Int]] = omsErrorCache.map {
    _.get(safePath).map { ee ⇒ ee.errorsInEditor }.getOrElse(Seq())
  }

  def updateErrorsInEditor(safePath: SafePath, n: Seq[Int]) = {
    omsErrorCache.update(omsErrorCache.now.updated(safePath, omsErrorCache.now.getOrElse(safePath, EditorErrors()).copy(errorsInEditor = n)))
  }

  def updateErrors(safePath: SafePath, errorsFromCompiler: Seq[ErrorFromCompiler]) = {
    omsErrorCache.update(omsErrorCache.now.updated(safePath, omsErrorCache.now.getOrElse(safePath, EditorErrors()).copy(errorsFromCompiler = errorsFromCompiler)))
  }
}

import TreeNodeTabs._

sealed trait TreeNodeTab {

  val safePathTab: Var[SafePath]
  val activity: Var[Activity] = Var(UnActive)

  val tabName = Var(safePathTab.now.name)
  val id: String = getUUID

  def activate = {
    activity() = Active
    onActivate()
  }

  def desactivate = {
    activity() = UnActive
    onDesactivate()
  }

  def onActivate: () ⇒ Unit = () ⇒ {}

  def onDesactivate: () ⇒ Unit = () ⇒ {}

  def extension: FileExtension = safePathTab.now.name

  // Get the file content to be saved
  def content: String

  def editor: Option[EditorPanelUI]

  def editable: Boolean

  def editing: Boolean

  def refresh(afterRefresh: () ⇒ Unit = () ⇒ {}): Unit

  def resizeEditor: Unit

  // controller to be added in menu bar
  val controlElement: TypedTag[HTMLElement]

  // Graphical representation
  val block: TypedTag[_ <: HTMLElement]
}

object TreeNodeTab {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  def save(safePath: SafePath, editorPanelUI: EditorPanelUI, afterSave: () ⇒ Unit) =
    editorPanelUI.synchronized {
      post()[Api].saveFile(safePath, editorPanelUI.code).call().foreach { _ ⇒
        afterSave()
      }
    }

  def oms(safePath: SafePath, initialContent: String) = new TreeNodeTab {

    lazy val safePathTab = Var(safePath)

    lazy val omsEditor = EditorPanelUI(safePath, FileExtension.OMS, initialContent)

    def editor = Some(omsEditor)

    omsEditor.initEditor

    def editable = true

    def editing = true

    override def onActivate: () ⇒ Unit = () => {
    }

    def content = omsEditor.code

    def refresh(onsaved: () ⇒ Unit) = save(safePathTab.now, omsEditor, onsaved)

    def resizeEditor = omsEditor.editor.resize()


    lazy val controlElement = {
      val compileDisabled = Var(false)
      val runOption = Var(false)

      def unsetErrors = setErrors(Seq())

      def setErrors(errors: Seq[ErrorWithLocation]) = {
        for {
          tab <- panels.treeNodeTabs.find(safePath)
          editor <- tab.editor
        } yield {
          editor.setErrors(errors)
        }
      }

      def setError(errorDataOption: Option[ErrorData]) = {
        compileDisabled.update(false)
        errorDataOption match {
          case Some(ce: CompilationErrorData) ⇒ setErrors(ce.errors)
          case _ =>
        }
      }
      lazy val validateButton = toggle(true, "Yes", "No")

      div(display.flex, flexDirection.row)(
        Rx {
          if (compileDisabled()) Waiter.waiter
          else
            button("Test", btn_default, onclick := { () ⇒
              unsetErrors
              compileDisabled.update(true)
              refresh(() =>
                post(timeout = 120 seconds, warningTimeout = 60 seconds)[Api].compileScript(ScriptData(safePathTab.now)).call().foreach { errorDataOption ⇒
                  setError(errorDataOption)
                })
            })
        },
        div(display.flex, flexDirection.row)(
          button("Run", btn_primary, marginLeft := 10, onclick := { () ⇒
            unsetErrors
            refresh(() ⇒
              post(timeout = 120 seconds, warningTimeout = 60 seconds)[Api].runScript(ScriptData(safePathTab.now), validateButton.position.now).call().foreach { execInfo ⇒
                org.openmole.gui.client.core.panels.executionPanel.dialog.show
              })
          }),
          Rx {
            if (runOption()) div(display.flex, flexDirection.row)(
              div("Script validation", giFontFamily, fontSize:="13px", marginLeft := 10, display.flex, alignItems.center),
              validateButton.render(border := "1px solid black", marginLeft := 10),
              button(ms("close closeRunOptions") +++ tabClose :+ (paddingBottom := 8) , `type` := "button", onclick := { () ⇒ runOption.update(false)})(raw("&#215")))
            else div(OMTags.options, pointer, marginLeft := 10, display.flex, alignItems.center)(onclick := {()=> runOption.update(true)})
          }
        )
      )
    }

    lazy val block = omsEditor.view
  }

  def html(safePath: SafePath, htmlContent: String) = new TreeNodeTab {
    lazy val safePathTab = Var(safePath)

    def content: String = htmlContent

    def editor = None

    def editable: Boolean = false

    def editing: Boolean = false

    def refresh(afterRefresh: () ⇒ Unit): Unit = () ⇒ {
    }

    def resizeEditor = {
    }

    lazy val controlElement: TypedTag[HTMLElement] = div()

    lazy val block: TypedTag[_ <: HTMLElement] = div(editorContainer +++ container)(
      div(panelClass +++ panelDefault)(
        div(panelBody)(
          ms("mdRendering") +++ (padding := 10),
          RawFrag(htmlContent)
        )
      )
    )
  }

  sealed trait EditableView {
    toString: String
  }

  object Raw extends EditableView {
    override def toString = "Raw"
  }

  object Table extends EditableView {
    override def toString = "Table"
  }

  object Plot extends EditableView {
    override def toString = "Plot"
  }

  sealed trait RowFilter

  object First100 extends RowFilter

  object Last100 extends RowFilter

  object All extends RowFilter

  case class IndexedAxis(title: String, fullSequenceIndex: Int)

  case class ClosureFilter(closure: String = "", axis: Seq[IndexedAxis] = Seq())

  def availableForError(sequence: SequenceData, axis: Seq[Int]) = sequence.header.zipWithIndex.filterNot {
    case (x, i) ⇒
      axis.contains(i)
  }.map { afe =>
    IndexedAxis(afe._1, afe._2)
  }

  def availableForClosureFilter(sequence: SequenceData, axis: Seq[Int], closure: String) =
    ClosureFilter(closure, sequence.header.zipWithIndex.filter {
      case (x, i) ⇒
        axis.contains(i)
    }.map { afe =>
      IndexedAxis(afe._1, afe._2)
    })


  def editable(
                safePath: SafePath,
                initialContent: String,
                editableSettings: EditableSettings): TreeNodeTab = new TreeNodeTab {

    lazy val safePathTab = Var(safePath)
    lazy val isEditing = Var(editableSettings.editing)

    Rx {
      editableEditor.setReadOnly(!isEditing())
    }

    def content: String = editableEditor.code

    val sequence = Var(editableSettings.sequence)
    val nbColumns = sequence.now.header.length

    def isCSV = DataUtils.isCSV(safePath)

    val filteredSequence = editableSettings.filter match {
      case First100 ⇒ sequence.now.content.take(100)
      case Last100 ⇒ sequence.now.content.takeRight(100)
      case _ ⇒ sequence.now.content
    }

    lazy val editableEditor = EditorPanelUI(safePath, extension, initialContent, if (isCSV) paddingBottom := 80 else emptyMod)

    def editor = Some(editableEditor)

    editableEditor.initEditor

    def editable = true

    def editing = isEditing.now

    def download(afterRefresh: () ⇒ Unit) = editor.synchronized {
      FileManager.download(
        safePathTab.now,
        (p: ProcessState) ⇒ {
        },
        (cont: String) ⇒ {
          editableEditor.setCode(cont)
          if (isCSV) {
            post()[Api].sequence(safePathTab.now).call().foreach {
              seq ⇒
                sequence() = seq
                afterRefresh()
            }
          }
          else afterRefresh()
        }
      )
    }

    def refresh(afterRefresh: () ⇒ Unit): Unit = {
      def saveTab = TreeNodeTab.save(safePathTab.now, editableEditor, afterRefresh)

      if (editing) {
        if (isCSV) {
          if (editableSettings.view == Raw) saveTab
        }
        else
          saveTab
      }
      else
        download(afterRefresh)
    }

    def resizeEditor = editableEditor.editor.resize()

    lazy val controlElement: TypedTag[HTMLElement] =
      div(
        Rx {
          if (isEditing()) div()
          else if (editableSettings.view == Raw) {
            button("Edit", btn_primary, onclick := {
              () ⇒
                isEditing() = !isEditing.now
            })
          }
          else div()
        }
      )

    lazy val editorView = editableEditor.view

    val switchString = editableSettings.view match {
      case Table ⇒ Raw.toString
      case _ ⇒ Table.toString
    }

    def switchView(newView: EditableView) = {

      def switch = panels.treeNodeTabs.switchEditableTo(this, editableSettings.copy(sequence = sequence.now, view = newView))

      newView match {
        case Table | Plot ⇒
          isEditing() = false
          switch
        case _ ⇒
          if (editing)
            refresh(() ⇒ {
              download(() ⇒ switch)
            })
          else switch
      }
    }

    def toView(newFilter: RowFilter) = panels.treeNodeTabs.switchEditableTo(this, editableSettings.copy(sequence = sequence.now, filter = newFilter))

    def toView(newAxis: Seq[Int]) = {
      val afe = editableSettings.error.map { _ => availableForError(sequence.now, newAxis) }.map {
        _.head
      }
      panels.treeNodeTabs.switchEditableTo(this, editableSettings.copy(sequence = sequence.now, axis = newAxis, error = afe))
    }

    def toView(newMode: PlotMode) = {
      val (seqs, ax) = newMode match {
        case SplomMode ⇒ (editableSettings.sequence, editableSettings.axis.take(5))
        case HeatMapMode => (sequence.now, 0 to sequence.now.header.size - 1)
        case _ ⇒ (sequence.now, editableSettings.axis.take(2))
      }

      panels.treeNodeTabs.switchEditableTo(this, editableSettings.copy(axis = ax, plotMode = newMode))
    }

    def toView(newError: Option[IndexedAxis]) = panels.treeNodeTabs.switchEditableTo(this, editableSettings.copy(sequence = sequence.now, error = newError))

    def toClosureView(newPlotClosure: Option[ClosureFilter]): Unit = {
      panels.treeNodeTabs.switchEditableTo(this, editableSettings.copy(plotClosure = newPlotClosure))
    }


    lazy val switchButton = radios(margin := 20)(
      selectableButton("Raw", editableSettings.view == Raw, onclick = () ⇒ switchView(Raw)),
      selectableButton("Table", editableSettings.view == Table, onclick = () ⇒ switchView(Table)),
      selectableButton("Plot", editableSettings.view == Plot, onclick = () ⇒ switchView(Plot))
    )

    lazy val filterRadios = radios(marginLeft := 40)(
      selectableButton("First 100", editableSettings.filter == First100, onclick = () ⇒ toView(First100)),
      selectableButton("Last 100", editableSettings.filter == Last100, onclick = () ⇒ toView(Last100)),
      selectableButton("All", editableSettings.filter == All, modifierSeq = btn_danger, onclick = () ⇒ toView(All))
    )

    val rowStyle: ModifierSeq = Seq(
      display.table,
      width := "100%"
    )

    lazy val colStyle: ModifierSeq = Seq(
      display.`table-cell`
    )

    lazy val axisCheckBoxes = checkboxes(colStyle +++ (margin := 20))(
      (for (
        a ← sequence.now.header.zipWithIndex
      ) yield {
        selectableButton(a._1, editableSettings.axis.contains(a._2), onclick = () ⇒ {
          val newAxis = editableSettings.plotMode match {
            case SplomMode ⇒ if (editableSettings.axis.contains(a._2)) editableSettings.axis.filterNot(_ == a._2) else editableSettings.axis :+ a._2
            case HeatMapMode => Seq()
            case _ ⇒ Seq(editableSettings.axis.last, a._2)
          }
          toView(newAxis)
        })
      }): _*
    )


    def errorCheckBox = checkboxes(marginLeft := 20)(
      (for (
        a ← availableForError(sequence.now, editableSettings.axis)
      ) yield {
        selectableButton(a.title, editableSettings.error == Some(a), onclick = () ⇒ {
          toView(Some(a))
        })
      }): _*
    )

    lazy val filterClosureRadios = checkboxes(marginLeft := 20)(
      (for (
        a ← availableForClosureFilter(sequence.now, editableSettings.axis, editableSettings.plotClosure.map {
          _.closure
        }.getOrElse("")).axis
      ) yield {
        selectableButton(a.title, editableSettings.plotClosure.map {
          _.axis
        }.getOrElse(Seq()).contains(a), onclick = () ⇒ {
          val axisSet = editableSettings.plotClosure.map {
            _.axis
          }.getOrElse(Seq())
          toClosureView(editableSettings.plotClosure.map {
            _.copy(closure = closureInput.value, axis = {
              if (axisSet.contains(a)) axisSet.filterNot(_ == a)
              else axisSet :+ a
            })
          })
        })
      }): _*
    )

    lazy val toggleFilter: ToggleButton = toggle(editableSettings.plotClosure.isDefined, "On", "Off", onToggled = () ⇒ {
      if (toggleFilter.position.now) toClosureView(Some(ClosureFilter(closureInput.value, editableSettings.plotClosure.map {
        _.axis
      }.getOrElse(Seq()))))
      else toClosureView(None)
    }
    )


    lazy val closureInput = input(placeholder := "Filter closure. Ex: x < 10", marginLeft := 10)(value := editableSettings.plotClosure.map {
      _.closure
    }.getOrElse("")).render

    lazy val inputFilterValidation = button(btn_primary, marginLeft := 10, "Apply", onclick := { () =>
      toClosureView(editableSettings.plotClosure.map {
        _.copy(closure = closureInput.value)
      })
    })

    lazy val toggleError: ToggleButton = toggle(editableSettings.error.isDefined, "On", "Off", onToggled = () ⇒ {
      if (toggleError.position.now) toView(availableForError(sequence.now, editableSettings.axis).headOption)
      else toView(None)
    })

    lazy val errorBar = editableSettings.plotMode match {
      case SplomMode | HeatMapMode ⇒ div()
      case _ ⇒
        scalatags.JsDom.tags.span(hForm(
          scalatags.JsDom.tags.span(
            toggleError.render,
            Rx {
              if (toggleError.position()) scalatags.JsDom.tags.span(errorCheckBox.render).render
              else {
                scalatags.JsDom.tags.span.render
              }
            }).render.withLabel("Error bar")
        ),
          scalatags.JsDom.tags.span(hForm(
            scalatags.JsDom.tags.span(
              toggleFilter.render,
              Rx {
                if (toggleFilter.position()) scalatags.JsDom.tags.span(filterClosureRadios.render, closureInput, inputFilterValidation).render
                else {
                  scalatags.JsDom.tags.span.render
                }
              }).render.withLabel("Filter")
          ))

        )
    }

    lazy val plotModeRadios = radios(marginLeft := 40)(
      selectableButton("Line", editableSettings.plotMode == XYMode, onclick = () ⇒ toView(XYMode)),
      selectableButton("Scatter", editableSettings.plotMode == ScatterMode, onclick = () ⇒ toView(ScatterMode)),
      selectableButton("SPLOM", editableSettings.plotMode == SplomMode, onclick = () ⇒ toView(SplomMode)),
      selectableButton("Heat map", editableSettings.plotMode == HeatMapMode, onclick = () ⇒ toView(HeatMapMode))
    )

    def jsClosure(value: String, col: Int) = {
      val closure = closureInput.value
      if (closure.isEmpty) true
      else {
        editableSettings.plotClosure.map {
          _.axis.find(_.fullSequenceIndex == col).map { pc =>
            closure.replace("x", value)
          }.map { cf =>
            scala.util.Try(scala.scalajs.js.eval(s"function func() { return ${cf};} func()").asInstanceOf[Boolean]).toOption.getOrElse(true)
          }.getOrElse(true)
        }.getOrElse(true)
      }
    }


    lazy val block: TypedTag[_ <: HTMLElement] = {
      div(
        if (isCSV) {
          editableSettings.view match {
            case Table ⇒ div(switchButton.render, filterRadios.render)
            case Plot ⇒
              div(
                vForm(
                  div(switchButton.render, filterRadios.render, plotModeRadios.render).render,
                  editableSettings.plotMode match {
                    case HeatMapMode => div().render
                    case _ => scalatags.JsDom.tags.span(axisCheckBoxes.render).render.withLabel("x|y axis")
                  },
                  errorBar.render
                ),
              )
            case _ ⇒ div(switchButton.render, div.render)
          }
        }
        else div,
        editableSettings.view match {
          case Table ⇒
            div(overflow := "auto", height := "90%")({
              if (!sequence.now.header.isEmpty && !filteredSequence.isEmpty) {
                val table =
                  scaladget.bootstrapnative.DataTable(
                    Some(scaladget.bootstrapnative.Table.Header(sequence.now.header)),
                    filteredSequence.map {
                      scaladget.bootstrapnative.DataTable.DataRow(_)
                    }.toSeq,
                    scaladget.bootstrapnative.Table.BSTableStyle(bordered_table, emptyMod), true)
                table.render(width := sequence.now.header.length * 90)
              }
              else div()
            }
            )
          case Raw ⇒ editorView
          case _ ⇒
            if (filteredSequence.size > 0) {
              if (filteredSequence.head.length >= editableSettings.axis.length) {
                val dataRow = filteredSequence.map {
                  scaladget.bootstrapnative.DataTable.DataRow(_)
                }.toSeq
                plot.Plot(
                  "",
                  Serie(editableSettings.axis.length, editableSettings.axis.foldLeft(Array[Dim]()) { (acc, col) ⇒
                    acc :+ Dim(DataTable.column(col, dataRow).values.filter(v => jsClosure(v, col)), sequence.now.header.lift(col).getOrElse(""))
                  }),
                  false,
                  editableSettings.plotMode,
                  editableSettings.error.map { i ⇒ Serie(1, Seq(Dim(DataTable.column(i.fullSequenceIndex, dataRow).values, sequence.now.header.lift(i.fullSequenceIndex).getOrElse("")))) }
                )

              }
              else div("No plot to display")
            }
            else div("No plot to display")
        }
      )
    }

  }
}

case class EditableSettings(
                             sequence: SequenceData,
                             view: EditableView,
                             filter: RowFilter,
                             editing: Boolean,
                             axis: Seq[Int],
                             plotMode: PlotMode,
                             error: Option[IndexedAxis],
                             plotClosure: Option[ClosureFilter]
                           )

object EditableSettings {
  def build(sequence: SequenceData,
            view: EditableView = Raw,
            filter: RowFilter = First100,
            editing: Boolean = false,
            axis: Seq[Int] = Seq(0, 1),
            plotMode: PlotMode = ScatterMode,
            error: Option[IndexedAxis] = None,
            plotClosure: Option[ClosureFilter] = None) = EditableSettings(sequence, view, filter, editing, axis, plotMode, error, plotClosure)
}

class TreeNodeTabs() {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  val tabs: Var[Seq[TreeNodeTab]] = Var(Seq())
  val timer: Var[Option[SetIntervalHandle]] = Var(None)
  val temporaryControl: Var[TypedTag[HTMLElement]] = Var(div())

  def stopTimerIfNoTabs = {
    if (tabs.now.isEmpty) {
      timer.map {
        _.foreach {
          clearInterval
        }
      }
      timer() = None
    }
  }

  def startTimerIfStopped =
    timer.now match {
      case None ⇒
        timer() = Some(setInterval(15000) {
          tabs.now.foreach {
            _.refresh()
          }
        })
      case _ ⇒
    }

  def setActive(tab: TreeNodeTab) = {
    if (tabs.now.contains(tab)) {
      unActiveAll
    }
    tab.activate
  }

  def isActive(safePath: SafePath) = tabs.now.filter {
    _.safePathTab.now == safePath
  }.map {
    _.activity
  }.headOption.getOrElse(Var(TreeNodeTabs.UnActive))

  def unActiveAll = tabs.map {
    _.foreach { t ⇒
      t.desactivate
    }
  }

  def ++(tab: TreeNodeTab) = {
    tabs() = tabs.now :+ tab
    startTimerIfStopped
    setActive(tab)
  }

  def removeTab(tab: TreeNodeTab) = {
    tab.desactivate
    val newTabs = tabs.now.filterNot {
      _ == tab
    }
    tabs() = newTabs
    if (tabs.now.isEmpty) temporaryControl() = div()
    newTabs.lastOption.map { t ⇒
      setActive(t)
    }
  }

  def --(tab: TreeNodeTab): Unit = tab.refresh(() ⇒ removeTab(tab))

  def --(safePath: SafePath): Unit = {
    find(safePath).map {
      removeTab
    }
  }

  def switchEditableTo(tab: TreeNodeTab, editableSettings: EditableSettings) = {
    val newTab = TreeNodeTab.editable(tab.safePathTab.now, tab.content, editableSettings)
    switchTab(tab, newTab)
  }

  def switchTab(tab: TreeNodeTab, to: TreeNodeTab) = {
    val index = {
      val i = tabs.now.indexOf(tab)
      if (i == -1) tabs.now.size
      else i
    }

    removeTab(tab)
    tabs() = tabs.now.take(index) ++ Seq(to) ++ tabs.now.takeRight(tabs.now.size - index)

    setActive(to)
  }

  def alterables: Seq[AlterableFileContent] = tabs.now.filter {
    _.editable
  }.map { t ⇒ AlterableFileContent(t.safePathTab.now, t.content) }

  def saveAllTabs(onsave: () ⇒ Unit) = {
    org.openmole.gui.client.core.post()[Api].saveFiles(alterables).call().foreach { s ⇒
      onsave()
    }
  }

  def checkTabs = tabs.now.foreach { t: TreeNodeTab ⇒
    org.openmole.gui.client.core.post()[Api].exists(t.safePathTab.now).call().foreach { e ⇒
      if (!e) removeTab(t)
    }
  }

  def rename(sp: SafePath, newSafePath: SafePath) = {
    find(sp).map { tab ⇒
      tab.tabName() = newSafePath.name
      tab.safePathTab() = newSafePath
    }
  }

  def find(safePath: SafePath) = tabs.now.find { t ⇒
    t.safePathTab.now == safePath
  }

  implicit def modToModSeq(m: Modifier): ModifierSeq = Seq(m)

  val render = div(
    //Headers
    Rx {
      val tabList = ul(nav +++ navTabs, tab_list_role)(
        for (t ← tabs()) yield {
          li(
            paddingTop := 35,
            presentation_role,
            `class` := {
              t.activity() match {
                case Active ⇒ "active"
                case _ ⇒ ""
              }
            }
          )(
            a(
              id := t.id,
              tab_role,
              pointer,
              t.activity() match {
                case Active ⇒ activeTab
                case _ ⇒ unActiveTab
              },
              data("toggle") := "tab", onclick := { () ⇒
                setActive(t)
              }
            )(
              button(ms("close") +++ tabClose, `type` := "button", onclick := { () ⇒ --(t) })(raw("&#215")),
              t.tabName()
            )
          )
        }
      ).render

      //Panes
      val tabDiv = div(tabContent)(
        for (t ← tabs()) yield {
          div(
            role := "tabpanel",
            ms("tab-pane " + {
              t.activity() match {
                case Active ⇒ "active"
                case _ ⇒ ""
              }
            }), id := t.id
          )({
            t.activity() match {
              case Active ⇒
                temporaryControl() = t.controlElement
                t.block
              case UnActive ⇒ div()
            }
          }
          )
        }
      )

      new Sortable(tabList, new SortableProps {
        override val onEnd = scala.scalajs.js.defined {
          (event: EventS) ⇒
            val oldI = event.oldIndex.asInstanceOf[Int]
            val newI = event.newIndex.asInstanceOf[Int]
            tabs() = tabs.now.updated(oldI, tabs.now(newI)).updated(newI, tabs.now(oldI))
            setActive(tabs.now(newI))
        }
      })

      div(role := "tabpanel")(
        tabList,
        tabDiv
      )
    }
  )

}
