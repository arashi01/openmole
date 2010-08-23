/*
 *  Copyright (c) 2008, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */
package org.simexplorer.core.workflow.methods.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import org.openide.util.Exceptions;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.event.UndoableEditListener;
import org.simexplorer.core.workflow.methods.EditorPanel;
import org.simexplorer.ui.tools.ActionsUtils;
import org.openmole.plugin.task.code.CodeTask;

public class CodeEditablePanel extends EditorPanel<CodeTask> {

    private DefaultListModel listModel;

    public CodeEditablePanel(Class<? extends CodeTask>... typesEditable) {
        super(typesEditable);
        listModel = new DefaultListModel();
        initComponents();
        this.add(jPanel1);
    }

    @Override
    public void addUndoableEditListener(UndoableEditListener listener) {
        textEditor.getDocument().addUndoableEditListener(listener);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        textEditor = new javax.swing.JTextPane();

        jButton1.setText("load from file");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        textEditor.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                textEditorCaretUpdate(evt);
            }
        });
        jScrollPane1.setViewportView(textEditor);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 290, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jButton1)
                        .addGap(183, 183, 183))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jButton1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 338, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 314, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 381, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void textEditorCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_textEditorCaretUpdate
// TF only done one time with apply change
/*        try {
            getObjectEdited().setCode(textEditor.getText());
        } catch (UserBadDataError ex) {
            Exceptions.printStackTrace(ex);
        } catch (InternalProcessingError ex) {
            Exceptions.printStackTrace(ex);
        }
 */
    }//GEN-LAST:event_textEditorCaretUpdate

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        JFileChooser fc = ActionsUtils.getJFileChooser("Browse", new File("."));
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            File in = fc.getSelectedFile();
            BufferedReader reader;
            Scanner s = null;
            try {
                s = new Scanner(in);
            } catch (FileNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            }
            StringBuilder builder = new StringBuilder();
            while (s.hasNextLine()) {
                builder.append(s.nextLine() + "\n");
            }
            textEditor.setText(builder.toString());
        }

    }//GEN-LAST:event_jButton1ActionPerformed

    @Override
    public void setObjectEdited(CodeTask method) {
        super.setObjectEdited(method);
        try {
            this.textEditor.setText(method.getCode());
        } catch (UserBadDataError ex) {
            Exceptions.printStackTrace(ex);
        } catch (InternalProcessingError ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public void applyChanges() {
        super.applyChanges();
        try {
            getObjectEdited().setCode(textEditor.getText());
        } catch (UserBadDataError ex) {
            Exceptions.printStackTrace(ex);
        } catch (InternalProcessingError ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextPane textEditor;
    // End of variables declaration//GEN-END:variables
}
