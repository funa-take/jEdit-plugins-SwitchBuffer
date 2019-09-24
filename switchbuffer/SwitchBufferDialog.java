/* {{{ header
* :tabSize=4:indentSize=4:noTabs=false:folding=explicit:collapseFolds=1:
*
* SwitchBufferDialog.java - Dialog for SwitchBuffer
* Copyright (C) 2003 Lee Turner
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
* }}}
*/
package switchbuffer;

//{{{ imports
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Vector;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.SwingUtilities;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.*;
import org.gjt.sp.jedit.gui.KeyEventTranslator;
//}}}

/**
 * Dialog for the SwitchBuffer plugin
 *
 * @author    <a href="mailto:lee@leeturner.org">Lee Turner</a>
 * @version   $Revision: 1.15 $ $Date: 2003/04/09 08:08:11 $
 */
public class SwitchBufferDialog extends JDialog
{
	//{{{ instance fields
	protected JList bufferList;
	protected JTextField bufferName;
	private JCheckBox recentCheck;
	private JCheckBox filePathMatching;
	private JCheckBox keywordMatching;
	private JButton okButton;
	private JButton closeButton;
	private Action switchAndHideAction;
	private Action hideAction;
	private Action nextBufferAction;
	private Action prevBufferAction;
	private Action closeBufferAction;
	private Action orderSelectAction;
	protected View parentView;
	protected String separator = File.separator;
	protected String commonRoot = null;//}}}
	
	//{{{ +SwitchBufferDialog(View) : <init>
	/**
	 * Constructor for the SwitchBufferDialog object
	 *
	 * @param view  The parent jEdit view
	 */
	public SwitchBufferDialog(View view)
	{
		super(view, jEdit.getProperty("options.switchbuffer.label"), true);
		parentView = view;
		createLayout();
		handleEvents();
	}//}}}
	
	//{{{ -createLayout() : void
	/**
	 * Creates all components on the dialog and lays them out.
	 */
	private void createLayout()
	{
		bufferName = new JTextField(jEdit.getProperty("switchbuffer.inputfield-text"));
		bufferName.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent evt) {
					if (ClassLoader.getSystemResource("org/gjt/sp/jedit/gui/UserKey.class")!=null){
						org.gjt.sp.jedit.gui.UserKey.consume(evt, 
							org.gjt.sp.jedit.gui.UserKey.ALLOW_CTRL | org.gjt.sp.jedit.gui.UserKey.ALLOW_SHIFT,
							org.gjt.sp.jedit.gui.UserKey.ALLOW_CTRL | org.gjt.sp.jedit.gui.UserKey.ALLOW_SHIFT,
							org.gjt.sp.jedit.gui.UserKey.ALLOW_CTRL | org.gjt.sp.jedit.gui.UserKey.ALLOW_SHIFT,
							org.gjt.sp.jedit.gui.UserKey.ALLOW_CTRL | org.gjt.sp.jedit.gui.UserKey.ALLOW_SHIFT,
							true, true, true, true, true);
						if (evt.isConsumed()){
							return;
						}
					}
				}
		});
		bufferList = new JList();
		bufferList.setSelectionMode(0);
		bufferList.setCellRenderer(getRenderer());
		bufferList.addMouseListener(
			new MouseListener()
			{
				public void mouseClicked(MouseEvent me)
				{
					if(me.getButton() == me.BUTTON1)
					{
						switchAndHide();
					}
					if(me.getButton() == me.BUTTON3)
					{
						int clickedIndex = bufferList.locationToIndex(me.getPoint());
						bufferList.setSelectedIndex(clickedIndex);
						closeBuffer();
					}
				}
				public void mousePressed(MouseEvent me) { }
				public void mouseEntered(MouseEvent me) { }
				public void mouseExited(MouseEvent me) { }
				public void mouseReleased(MouseEvent me) { }
			});
		recentCheck = new JCheckBox("Order by Recent File");
		recentCheck.setMnemonic(KeyEvent.VK_N);
		filePathMatching = new JCheckBox("Match on File Path");
		filePathMatching.setMnemonic(KeyEvent.VK_P);
		keywordMatching = new JCheckBox("Match with Keywords");
		keywordMatching.setMnemonic(KeyEvent.VK_Y);
		
		Dimension buttonSize = new Dimension(100, 25);
		okButton = new JButton("OK");
		okButton.setPreferredSize(buttonSize);
		closeButton = new JButton("Cancel");
		closeButton.setPreferredSize(buttonSize);
		JPanel bufferNamePanel = new JPanel();
		bufferNamePanel.setLayout(new BoxLayout(bufferNamePanel, BoxLayout.X_AXIS));
		bufferNamePanel.add(bufferName);
		getContentPane().add(bufferNamePanel, "North");
		getContentPane().add(new JScrollPane(bufferList), "Center");
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
		buttonsPanel.add(Box.createHorizontalGlue());
		buttonsPanel.add(filePathMatching);
		buttonsPanel.add(keywordMatching);
		buttonsPanel.add(recentCheck);
		buttonsPanel.add(okButton);
		buttonsPanel.add(closeButton);
		getContentPane().add(buttonsPanel, "South");
		pack();
		GUIUtilities.loadGeometry(this, "switchbuffer.dialog");
	}//}}}
	
	//{{{ -getRenderer() : ListCellRenderer
	/**
	 * Gets the ListCellRenderer for the file list based on the setting in the
	 * options pane.
	 *
	 * @return   The ListCellRenderer to be used in the file list
	 */
	protected ListCellRenderer getRenderer()
	{
		return
		new DefaultListCellRenderer()
		{
			public Component getListCellRendererComponent(JList jlist, Object obj, int index, boolean isSelected, boolean cellHasFocus)
			{
				Buffer buff = (Buffer)obj;
				if(jEdit.getBooleanProperty("switchbuffer.options.show-buffer-directories"))
				{
					if(jEdit.getBooleanProperty("switchbuffer.options.show-intelligent-buffer-directories"))
					{
						if(commonRoot == null)
						{
							setTitle(jEdit.getProperty("options.switchbuffer.label"));
							setText(buff.toString());
						}
						else
						{
							String parent = commonRoot.substring(0, commonRoot.length() - 1);// remove trailing '/' or '\'
							int parentIndex = parent.lastIndexOf(separator);
							if(parentIndex != -1)
							{
								parent = parent.substring(parentIndex);
							}
							else if(parent.length() == 0)
							{//root on *nix i think....
								
								parent = separator;
							}
							
							if(buff.getDirectory().equals(commonRoot))
							{
								setText(buff.getName() + " (" + parent + ")");
							}
							else
							{
								setText(buff.getName() + " (" + parent + separator + buff.getDirectory().substring(commonRoot.length()) + ")");
							}
							
							if(parentIndex != -1)
							{
								setTitle(jEdit.getProperty("options.switchbuffer.label") + " - " + commonRoot.substring(0, parentIndex));
							}
							else
							{
								setTitle(jEdit.getProperty("options.switchbuffer.label") + " - " + parent);
							}
							
						}
					}
					else
					{
						setText(buff.toString());
					}
				}
				else
				{
					setText(buff.getName());
				}
				
				if(jEdit.getBooleanProperty("switchbuffer.options.show-buffer-icons"))
				{
					setIcon(buff.getIcon());
				}
				
				if(isSelected)
				{
					setBackground(jlist.getSelectionBackground());
					if(jEdit.getBooleanProperty("switchbuffer.options.show-buffer-colours"))
					{
						setForeground(SwitchBufferUtils.getColour(buff.getName()));
					}
					else
					{
						setForeground(jlist.getSelectionForeground());
					}
				}
				else
				{
					setBackground(jlist.getBackground());
					if(jEdit.getBooleanProperty("switchbuffer.options.show-buffer-colours"))
					{
						setForeground(SwitchBufferUtils.getColour(buff.getName()));
					}
					else
					{
						setForeground(jlist.getForeground());
					}
				}
				setEnabled(jlist.isEnabled());
				setFont(jlist.getFont());
				setOpaque(true);
				return this;
			}
		};
	}//}}}
	
	//{{{ -handleEvents() : void
	/**
	 * Adds all the window and keyboard events that need to be trapped or listened
	 * for what the dialog is visible.
	 */
	private void handleEvents()
	{
		addWindowListener(
			new WindowAdapter()
			{
				public void windowActivated(WindowEvent windowevent)
				{
					commonRoot = null;
					bufferList.setCellRenderer(getRenderer());
					if(jEdit.getProperty("switchbuffer.file-suffix-switch.filename") != "")
					{
						bufferName.setText(jEdit.getProperty("switchbuffer.file-suffix-switch.filename"));
					}
					else
					{
						if(jEdit.getBooleanProperty("switchbuffer.options.remember-previous-buffer"))
						{
							if((jEdit.getProperty("switchbuffer.last-open-file") != null) || (jEdit.getProperty("switchbuffer.last-open-file") != ""))
							{
								bufferName.setText(jEdit.getProperty("switchbuffer.last-open-file"));
							}
						}
						else
						{
							if(!jEdit.getBooleanProperty("switchbuffer.options.remember-text-entered"))
							{
								bufferName.setText("");
							}
						}
					}
					refreshBufferList(bufferName.getText());
					bufferName.grabFocus();
					bufferName.selectAll();
				}
				
				public void windowDeactivated(WindowEvent windowevent)
				{
					if(jEdit.getBooleanProperty("switchbuffer.options.general.lost-focus"))
					{
						if(isVisible())
						{
							setVisible(false);
						}
					}
				}
			});
		addComponentListener(
			new ComponentAdapter()
			{
				public void componentResized(ComponentEvent componentevent)
				{
					GUIUtilities.saveGeometry(SwitchBufferDialog.this, "switchbuffer.dialog");
				}
				
				public void componentMoved(ComponentEvent componentevent)
				{
					GUIUtilities.saveGeometry(SwitchBufferDialog.this, "switchbuffer.dialog");
				}
			});
		bufferName.getDocument().addDocumentListener(
			new DocumentListener()
			{
				public void insertUpdate(DocumentEvent documentevent)
				{
					refreshBufferList(documentevent);
				}
				
				public void changedUpdate(DocumentEvent documentevent)
				{
					refreshBufferList(documentevent);
				}
				
				public void removeUpdate(DocumentEvent documentevent)
				{
					refreshBufferList(documentevent);
				}
				
				public void refreshBufferList(DocumentEvent documentevent)
				{
					commonRoot = null;
					Document document = documentevent.getDocument();
					String s = null;
					try
					{
						s = document.getText(0, document.getLength());
					}
					catch(BadLocationException badlocationexception)
					{
						return;
					}
					SwitchBufferDialog.this.refreshBufferList(s);
				}
			});
		switchAndHideAction =
		new AbstractAction("switch-and-hide")
		{
			public void actionPerformed(ActionEvent actionevent)
			{
				switchAndHide();
			}
		};
		closeBufferAction =
		new AbstractAction("close-buffer")
		{
			public void actionPerformed(ActionEvent actionevent)
			{
				closeBuffer();
			}
		};
		hideAction =
		new AbstractAction("hide")
		{
			public void actionPerformed(ActionEvent actionevent)
			{
				setVisible(false);
			}
		};
		nextBufferAction =
		new AbstractAction("select-next-buffer")
		{
			public void actionPerformed(ActionEvent actionevent)
			{
				int listSize = bufferList.getModel().getSize();
				int selectedIndex = bufferList.getSelectedIndex();
				if(listSize < 1)
				{
					return;
				}
				if(selectedIndex == listSize - 1)
				{
					selectedIndex = 0;
				}
				else
				{
					selectedIndex++;
				}
				bufferList.setSelectedIndex(selectedIndex);
				bufferList.ensureIndexIsVisible(selectedIndex);
			}
		};
		prevBufferAction =
		new AbstractAction("select-previous-buffer")
		{
			public void actionPerformed(ActionEvent actionevent)
			{
				int selectedIndex = bufferList.getSelectedIndex();
				int listSize = bufferList.getModel().getSize();
				if(listSize < 1)
				{
					return;
				}
				if(selectedIndex < 1)
				{
					selectedIndex = listSize - 1;
				}
				else
				{
					selectedIndex--;
				}
				bufferList.setSelectedIndex(selectedIndex);
				bufferList.ensureIndexIsVisible(selectedIndex);
			}
		};
		orderSelectAction = new AbstractAction("select-order"){
			public void actionPerformed(ActionEvent actionevent)
			{
				jEdit.setBooleanProperty("switchbuffer.options.recent-order",recentCheck.isSelected());
				refreshBufferList(bufferName.getText());
				SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							bufferName.requestFocus();
						}
				});
				
			}
		};
		filePathMatching.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent actionevent)
				{
					jEdit.setBooleanProperty("switchbuffer.options.filePathMatching", filePathMatching.isSelected());
					refreshBufferList(bufferName.getText());
					SwingUtilities.invokeLater(new Runnable(){
							public void run(){
								bufferName.requestFocus();
							}
					});
				}
		});
		keywordMatching.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent actionevent)
				{
					jEdit.setBooleanProperty("switchbuffer.options.keywordMatching", keywordMatching.isSelected());
					refreshBufferList(bufferName.getText());
					SwingUtilities.invokeLater(new Runnable(){
							public void run(){
								bufferName.requestFocus();
							}
					});
				}
		});
		recentCheck.addActionListener(orderSelectAction);
		okButton.addActionListener(switchAndHideAction);
		closeButton.addActionListener(hideAction);
		bufferName.addActionListener(switchAndHideAction);
		
		KeyStroke escKeystroke = KeyStroke.getKeyStroke("ESCAPE");
		KeyStroke upKeystroke = KeyStroke.getKeyStroke("UP");
		KeyStroke downKeystroke = KeyStroke.getKeyStroke("DOWN");
		// funa edit
		// KeyStroke altiKeystroke = KeyStroke.getKeyStroke("alt I");
		KeyStroke altiKeystroke = KeyStroke.getKeyStroke(
			KeyEvent.VK_I,
			KeyEventTranslator.getModifierBeforeTranslate(KeyEvent.ALT_MASK));
		// KeyStroke altkKeystroke = KeyStroke.getKeyStroke("alt K");
		KeyStroke altkKeystroke = KeyStroke.getKeyStroke(
			KeyEvent.VK_K,
			KeyEventTranslator.getModifierBeforeTranslate(KeyEvent.ALT_MASK));
		// KeyStroke althKeystroke = KeyStroke.getKeyStroke("alt H");
		KeyStroke althKeystroke = KeyStroke.getKeyStroke(
			KeyEvent.VK_H,
			KeyEventTranslator.getModifierBeforeTranslate(KeyEvent.ALT_MASK));
		// KeyStroke ctrlwKeystroke = KeyStroke.getKeyStroke("control W");
		KeyStroke ctrlwKeystroke = KeyStroke.getKeyStroke(
			KeyEvent.VK_W,
			KeyEventTranslator.getModifierBeforeTranslate(KeyEvent.CTRL_MASK));
		
		// KeyStroke alt0Keystroke = KeyStroke.getKeyStroke("alt 0");
		KeyStroke alt0Keystroke = KeyStroke.getKeyStroke(
			KeyEvent.VK_0,
			KeyEventTranslator.getModifierBeforeTranslate(KeyEvent.ALT_MASK));
		
		// Funa Edit
		// InputMap inputmap = bufferName.getInputMap(JTextField.WHEN_IN_FOCUSED_WINDOW);
		InputMap inputmap = bufferName.getInputMap(JTextField.WHEN_FOCUSED);
		inputmap.put(escKeystroke, hideAction.getValue(Action.NAME));
		inputmap.put(althKeystroke, hideAction.getValue(Action.NAME));
		inputmap.put(upKeystroke, prevBufferAction.getValue(Action.NAME));
		inputmap.put(altiKeystroke, prevBufferAction.getValue(Action.NAME));
		inputmap.put(downKeystroke, nextBufferAction.getValue(Action.NAME));
		inputmap.put(altkKeystroke, nextBufferAction.getValue(Action.NAME));
		inputmap.put(ctrlwKeystroke, closeBufferAction.getValue(Action.NAME));
		inputmap.put(alt0Keystroke, closeBufferAction.getValue(Action.NAME));
		
		ActionMap actionmap = bufferName.getActionMap();
		actionmap.put(hideAction.getValue(Action.NAME), hideAction);
		actionmap.put(prevBufferAction.getValue(Action.NAME), prevBufferAction);
		actionmap.put(nextBufferAction.getValue(Action.NAME), nextBufferAction);
		actionmap.put(closeBufferAction.getValue(Action.NAME), closeBufferAction);
	}//}}}
	
	//{{{ +refreshBufferList(String) : void
	/**
	 * Updates the file list based on the string typed in the file name textbox.
	 * This takes into account whether the user has specified to ignore case or
	 * not.
	 *
	 * @param textToMatch  Description of the Parameter
	 */
	public void refreshBufferList(String textToMatch)
	{
		int oldIndex = bufferList.getSelectedIndex();
		
		if(textToMatch == null || textToMatch.trim().length() == 0)
		{
			bufferList.setListData(SwitchBufferPlugin.getBuffersByRecentOrder());
			if(bufferList.getModel().getSize() > 0){
				bufferList.setSelectedIndex(getIndex(oldIndex));
			}
		}
		
		Buffer buffers[] = SwitchBufferPlugin.getBuffersByRecentOrder();
		Vector vector = new Vector(buffers.length);
		boolean flag = jEdit.getBooleanProperty("switchbuffer.options.ignore-case");
		String matching = null;
		if(jEdit.getProperty("switchbuffer.file-suffix-switch.filename") != "")
		{
			matching = "BEGINNING";
		}
		else
		{
			matching = jEdit.getProperty("switchbuffer.options.filenameMatching");
		}
	textToMatch = flag ? textToMatch.toLowerCase() : textToMatch;
		for(int i = 0; i < buffers.length; i++)
		{
			boolean match = false;
			
			// funa edit
			String bufferName = null;
			boolean filePathMatching = jEdit.getBooleanProperty("switchbuffer.options.filePathMatching", false);
			boolean keywordMatching = jEdit.getBooleanProperty("switchbuffer.options.keywordMatching", false);
			
			if (matching.equals("ANYWHERE") && filePathMatching) {
				bufferName = buffers[i].getPath();
			} else {
				bufferName = buffers[i].getName();
			}
			if (flag) {
				bufferName = bufferName.toLowerCase();
			}
			
			if (matching.equals("ANYWHERE") && keywordMatching) {
				String[] keywords = textToMatch.trim().split("\\s+");
				boolean allFind = true;
				for(int keyIndex = 0; keyIndex < keywords.length; keyIndex++) {
					if (bufferName.indexOf(keywords[keyIndex]) == -1) {
						allFind = false;
						break;
					}
				}
				if (allFind) {
					match = true;
				}
			} else if(matching.equals("ANYWHERE") && bufferName.indexOf(textToMatch) != -1)
			{
				match = true;
			}
			else if(matching.equals("BEGINNING") && bufferName.startsWith(textToMatch))
			{
				match = true;
			}
			else if(matching.equals("SUBSEQUENCE") && SwitchBufferUtils.subSequenceMatch(textToMatch, bufferName))
			{
				match = true;
			}
			
			if(match == true)
			{
				if(jEdit.getBooleanProperty("switchbuffer.options.remove-active-buffer"))
				{
					if(!(parentView.getBuffer().equals(buffers[i])))
					{
						vector.add(buffers[i]);
					}
				}
				else
				{
					vector.add(buffers[i]);
				}
			}
		}
		
		commonRoot = null;
		commonRoot = SwitchBufferUtils.findCommonRoot(vector);
		
		bufferList.setListData(vector);
		if(bufferList.getModel().getSize() > 0){
			bufferList.setSelectedIndex(getIndex(oldIndex));
		}
		
	}//}}}
	
	//{{{ +switchAndHide() : void
	/**
	 * Method to switch to the selected buffer in the list and hide
	 * the SwitchBuffer dialog.
	 */
	public void switchAndHide()
	{
		Buffer selectedBuffer = (Buffer)bufferList.getSelectedValue();
		Buffer parentBuffer = parentView.getBuffer();
		if(selectedBuffer == null)
		{
			return;
		}
		parentView.setBuffer(selectedBuffer);
		
		if(jEdit.getBooleanProperty("switchbuffer.options.remember-previous-buffer"))
		{
			jEdit.setTemporaryProperty("switchbuffer.last-open-file", parentBuffer.getName());
		}
		
		if(isVisible())
		{
			setVisible(false);
		}
	} //}}}
	
	//{{{ +closeBuffer() : void
	/**
	 * Method to close the selected jEdit buffer and refresh the
	 * SwitchBuffer file list.
	 */
	public void closeBuffer()
	{
		Buffer selectedBuffer = (Buffer)bufferList.getSelectedValue();
		jEdit.closeBuffer(parentView, selectedBuffer);
		
		refreshBufferList(bufferName.getText());
		SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					requestFocus();
					bufferName.grabFocus();
					bufferName.selectAll();
				}
		});
	} //}}}
	
	protected int getIndex(int oldIndex){
		int size = bufferList.getModel().getSize();
		int index = oldIndex;
		
		if (size <= 0){
			return -1;
		}
		
		if (index < 0){
			index = 0;
		}
		
		if (index >= size){
			index = size - 1;
		}
		return index;
	}
	
	@Override
	public void setVisible(boolean visible){
		recentCheck.setSelected(jEdit.getBooleanProperty("switchbuffer.options.recent-order", false));
		filePathMatching.setSelected(jEdit.getBooleanProperty("switchbuffer.options.filePathMatching", false));
		keywordMatching.setSelected(jEdit.getBooleanProperty("switchbuffer.options.keywordMatching", false));
		boolean selectedAnywhere = "ANYWHERE".equals(jEdit.getProperty("switchbuffer.options.filenameMatching"));
		filePathMatching.setEnabled(selectedAnywhere);
		keywordMatching.setEnabled(selectedAnywhere);
		super.setVisible(visible);
	}
}

