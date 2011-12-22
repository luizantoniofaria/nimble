/*
Demoiselle Framework
Copyright (C) 2011 SERPRO
============================================================================
This file is part of Demoiselle Framework.

Demoiselle Framework is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public License version 3
as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU Lesser General Public License version 3
along with this program; if not,  see <http://www.gnu.org/licenses/>
or write to the Free Software Foundation, Inc., 51 Franklin Street,
Fifth Floor, Boston, MA  02110-1301, USA.
============================================================================
Este arquivo é parte do Framework Demoiselle.

O Framework Demoiselle é um software livre; você pode redistribuí-lo e/ou
modificá-lo dentro dos termos da GNU LGPL versão 3 como publicada pela Fundação
do Software Livre (FSF).

Este programa é distribuído na esperança que possa ser útil, mas SEM NENHUMA
GARANTIA; sem uma garantia implícita de ADEQUAÇÃO a qualquer MERCADO ou
APLICAÇÃO EM PARTICULAR. Veja a Licença Pública Geral GNU/LGPL em português
para maiores detalhes.

Você deve ter recebido uma cópia da GNU LGPL versão 3, sob o título
"LICENCA.txt", junto com esse programa. Se não, acesse <http://www.gnu.org/licenses/>
ou escreva para a Fundação do Software Livre (FSF) Inc.,
51 Franklin St, Fifth Floor, Boston, MA 02111-1301, USA.
*/
package br.gov.frameworkdemoiselle.tools.nimble.gui.wizard.tools

import java.awt.BorderLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.util.prefs.Preferences

import javax.swing.BorderFactory
import javax.swing.JOptionPane
import javax.swing.JScrollPane
import javax.swing.JTextField

import net.miginfocom.swing.MigLayout

import br.gov.frameworkdemoiselle.tools.nimble.DemoiselleNimble
import br.gov.frameworkdemoiselle.tools.nimble.gui.Variable 
import br.gov.frameworkdemoiselle.tools.nimble.gui.wizard.WizardPanel 
import br.gov.frameworkdemoiselle.tools.nimble.util.BooleanUtil
import br.gov.frameworkdemoiselle.tools.nimble.util.ConfigUtil
import br.gov.frameworkdemoiselle.tools.nimble.util.FileUtil
import br.gov.frameworkdemoiselle.tools.nimble.util.StringUtil

class TemplateVariablesStep2 extends WizardPanel {
	
	final String PREF_DEST_FOLDER = "dest";
	
	def varList = []
	
	JScrollPane panel 
	JTextField outputPathText
	KeyAdapter keyHandler
	
//	public final static Cursor busyCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
//	public final static Cursor defaultCursor = Cursor.getDefaultCursor()
	
	TemplateVariablesStep2() {
		prefs = Preferences.userNodeForPackage(TemplateVariablesStep2.class)
		keyHandler = new KeyAdapter() {
			void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					parentFrame?.proceed()
				}
			}
		}
	}
	
	void onLoad() {
		if (panel != null) {
			removeAll()
			updateUI()
		}
		title = context.template?.name ?: ""
		description = context.template?.description ?: ""
		if (context.template) {
			String confFileName = FileUtil.addSep(context.template.path) + "template.conf"
			varList = ConfigUtil.loadVars(varList, confFileName)
		}
		mountPanel()
		loadPreferences()
		firstField = outputPathText
	}
	
	Boolean validateForm() {
		for (Variable v: varList) {
			if (v.required) {
				if (v.component.textValue.trim().equals("")) {
					JOptionPane.showMessageDialog(this, 
						"Please fill all required fields!", title, JOptionPane.WARNING_MESSAGE)
					v.component.visualComponent.requestFocus()
					return false
				}
			}
		}
		return true
	}
	
	void finish() {
		if (validateForm()) {
			
//			getRootPane().setCursor(busyCursor)
//			this.setCursor(busyCursor);
//			Thread.sleep(5000)
//			this.setCursor(defaultCursor);
//			getRootPane().setCursor(defaultCursor)
			
			if (new File(outputPathText.text).exists()) {
				if (!new File(outputPathText.text).isDirectory()) {
					JOptionPane.showMessageDialog(this, 
						"Please inform a valid Output Folder", title, JOptionPane.WARNING_MESSAGE)
					return
				}
			} else {
				def response = JOptionPane.showConfirmDialog(this, 
					"Output Folder does not exist. Create it?", title, JOptionPane.OK_CANCEL_OPTION, 1)
				if (response == JOptionPane.CANCEL_OPTION)
					return
				else if (response == JOptionPane.OK_OPTION)
					new File(outputPathText.text).mkdirs()
			}
			
			def vars = [:]
			varList.each { v ->
				vars.put v.name, v.component.textValue
			}
			
			DemoiselleNimble nimble = new DemoiselleNimble(true)
			boolean result = nimble.applyTemplates(context.template?.folderName, outputPathText.text, vars)
			
			savePreferences()
			
			if (result)
				JOptionPane.showMessageDialog(this,
					"Template processing was concluded successfully!", title, JOptionPane.INFORMATION_MESSAGE)
			else
				JOptionPane.showMessageDialog(this,
					"An error occurred while processing the template.", title, JOptionPane.ERROR_MESSAGE)

			System.exit(0)
		}
	}
	
	void mountPanel() {
		panel = swing.scrollPane(border: null) {
			panel(layout: new MigLayout('insets 10, fillx', "[pref!][grow,fill][]",""), constraints:'grow') {
				def lof = label("1. Output Folder*:", displayedMnemonic:'1');
				outputPathText = textField()
				lof.setLabelFor(outputPathText)
				button(text: 'Browse...', constraints: 'wrap', mnemonic:'R', actionPerformed: { selectOutputFolder() })
				outputPathText.addKeyListener(keyHandler)
				varList.eachWithIndex { v, i ->
					
					def defaultValue = ""
					if (v.defaultValueClass != null) {
						def dvc = Class.forName(v.defaultValueClass).newInstance()
						defaultValue = dvc.getDefault()
					} else if (v.defaultValue != null) {
						defaultValue = v.defaultValue
					}
					def lab = label(text:"${i + 2}. ${v.label}${v.required ? '*' : ''}:")
					if (i < 10)
						lab.setDisplayedMnemonic(48 + i + 2)
					
					def component
					
					// TODO: dar uma melhorada nesse trecho
					if ("comboBox".equalsIgnoreCase(v.component?.type)) {
					/*switch (v.component.type) {
						case "comboBox":*/
						def comboItems = ""
						if (v.listClass != null) {
							def lc = Class.forName(v.listClass).newInstance()
							comboItems = lc.getList()
						} else if (v.component.attributes.get("items"))
							comboItems = v.component.attributes.get("items")
						def comboEditable = v.component.attributes.get("editable") ?: false
						component = comboBox(items:comboItems, editable:comboEditable, constraints: 'span 2, wrap')
					} else if ("checkBox".equalsIgnoreCase(v.component?.type)) {
					/*		break
						case "checkBox":*/
						def checkSelected = v.component.attributes.get("selected") ?: false
						component = checkBox(text:'', selected:checkSelected, constraints: 'wrap');
					} else if ("boolean".equalsIgnoreCase(v.dataType)) {
						Boolean checkSelected = BooleanUtil.parseString(defaultValue) ?: false
						component = checkBox(text:null, selected:checkSelected, constraints: 'wrap');
					} else {
					/*		break
						case "text":
						default:*/
						def passed = (context.variables?.size() > i ? context.variables[i] : null)
						component = textField(text:(passed ?: defaultValue), constraints: 'span 2, wrap', columns: 30)
					/*		break;
					}*/
					}

					lab.setLabelFor(component)
					v.component.attributes.each { key, value ->
						println "$key -- $value"
						if (key != "editable" && key != "items" && key != "selected")
							component.setProperty(key, value)
					}
					v.component.visualComponent = component
					component.addKeyListener(keyHandler)
				}
			}
		}
		panel.setViewportBorder(BorderFactory.createEmptyBorder())
		add(panel, BorderLayout.CENTER)
	}
	
	void selectOutputFolder() {
		String dir = (new File(outputPathText.text).isDirectory()) ? outputPathText.text : FileUtil.getCurrentDir()
		String destFolder = FileUtil.chooseDir(this, "Select output folder", dir)
		if (destFolder != null) {
			outputPathText.setText(destFolder)
		}
	}
	
	public void loadPreferences() {
		outputPathText.setText(context.outputPath ?: prefs?.get(PREF_DEST_FOLDER, ""))
		String stringVars = prefs?.get(context.template?.name, "[:]")
		def mapVars = StringUtil.convertKeyValueStringToMap(stringVars)
		for (Variable v: varList) {
//			println v.name + "->" + mapVars[v.name]
			v.component.textValue = mapVars[v.name]
		}
	}
	
	public void savePreferences() {
		prefs?.put(PREF_DEST_FOLDER, outputPathText.getText())
		def vars = [:]
		for (Variable v: varList) {
			vars.put v.name, v.component.textValue
		}
		prefs.put context.template?.name, StringUtil.convertMapToKeyValueString(vars)
	}
	
	public static void main(String[] args) {
		//Toolkit tk = Toolkit.getDefaultToolkit( )
		//tk.addAWTEventListener(WindowSaver.getInstance( ), AWTEvent.WINDOW_EVENT_MASK)
		new TemplateVariablesStep2().setVisible true
	}
	
}