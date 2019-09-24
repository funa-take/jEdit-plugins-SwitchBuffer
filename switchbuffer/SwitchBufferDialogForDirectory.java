/* {{{ header
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
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.io.VFSFile;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.browser.VFSBrowser;
import org.gjt.sp.jedit.MiscUtilities;
/**
 * Dialog for the SwitchBuffer plugin
 *
 */
public class SwitchBufferDialogForDirectory extends SwitchBufferDialog
{
  protected DirectorySearcher searcher = null;
  protected RefreshRunnable refreshRunnable = null;
  protected final int MAX_SHOW = 100;
  
  /**
   * Constructor for the SwitchBufferDialog object
   *
   * @param view  The parent jEdit view
   */
  public SwitchBufferDialogForDirectory(View view)
  {
    super(view);
  }
  
  
  public void setView(View view) {
    this.parentView = view;
  }
  
  protected ListCellRenderer getRenderer()
  {
    return
    new DefaultListCellRenderer()
    {
      public Component getListCellRendererComponent(JList jlist, Object obj, int index, boolean isSelected, boolean cellHasFocus)
      {
        String path = obj.toString();
        String name = MiscUtilities.getFileName(path);
        String directory = MiscUtilities.getParentOfPath(path);
        String toString = name + " (" + MiscUtilities.abbreviateView(directory) + ')';
        
        if(jEdit.getBooleanProperty("switchbuffer.options.show-buffer-directories"))
        {
          if(jEdit.getBooleanProperty("switchbuffer.options.show-intelligent-buffer-directories"))
          {
            if(commonRoot == null)
            {
              setTitle(jEdit.getProperty("options.switchbuffer.label"));
              setText(toString);
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
              
              if(directory.equals(commonRoot))
              {
                setText(name + " (" + parent + ")");
              }
              else
              {
                setText(name + " (" + parent + separator + directory.substring(commonRoot.length()) + ")");
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
            setText(toString);
          }
        }
        else
        {
          setText(name);
        }
        
        if(isSelected)
        {
          setBackground(jlist.getSelectionBackground());
          if(jEdit.getBooleanProperty("switchbuffer.options.show-buffer-colours"))
          {
            setForeground(SwitchBufferUtils.getColour(name));
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
            setForeground(SwitchBufferUtils.getColour(name));
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
  }
  
  public void refreshBufferList(String textToMatch)
  {
    int oldIndex = bufferList.getSelectedIndex();
    if(textToMatch == null || textToMatch.trim().length() == 0)
    {
      if (searcher.getFilesSize() < MAX_SHOW) {
        commonRoot = searcher.getSearchDirectory();
        bufferList.setListData(searcher.getFiles());
      }
      if(bufferList.getModel().getSize() > 0){
          bufferList.setSelectedIndex(getIndex(oldIndex));
        }
      // return;
    }
    
    String[] buffers = searcher.getFiles();
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
        bufferName = buffers[i];
      } else {
        bufferName = MiscUtilities.getFileName(buffers[i]);
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
          if(!(parentView.getBuffer().getPath().equals(buffers[i])))
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
    
    while(vector.size() >= MAX_SHOW) {
      vector.remove(MAX_SHOW - 1);
    }
    commonRoot = searcher.getSearchDirectory();
    bufferList.setListData(vector);
    if(bufferList.getModel().getSize() > 0){
      bufferList.setSelectedIndex(getIndex(oldIndex));
    }
  }
  
  public void setVisible(boolean visible) {
    if (visible) {
      searcher = new DirectorySearcher(parentView);
      (new Thread(searcher)).start();
      refreshRunnable = new RefreshRunnable(this, searcher);
      (new Thread(refreshRunnable)).start();
      
    } else {
      searcher.stop();
      refreshRunnable.stop();
    }
    super.setVisible(visible);
  }
  
  public void switchAndHide()
  {
    String selectedPath = bufferList.getSelectedValue().toString();
    Buffer parentBuffer = parentView.getBuffer();
    if(selectedPath == null)
    {
      return;
    }
    // parentView.setBuffer(selectedBuffer);
    jEdit.openFile(parentView, selectedPath);
    
    if(jEdit.getBooleanProperty("switchbuffer.options.remember-previous-buffer"))
    {
      jEdit.setTemporaryProperty("switchbuffer.last-open-file", MiscUtilities.getFileName(selectedPath));
    }
    
    if(isVisible())
    {
      setVisible(false);
    }
  }
  
  static class DirectorySearcher implements Runnable {
    private boolean stop = false;
    private Vector<String> files = new Vector<String>();
    private View view;
    private String searchDirectory = null;
    
    public DirectorySearcher(View view) {
      this.view = view;
    }
    
    public void run() {
      VFSBrowser browser = (VFSBrowser)view.getDockableWindowManager().getDockable("vfs.browser");
      if (browser == null) return;
      
      String dir = null;
      VFSFile[] selectedFiles = browser.getSelectedFiles();
      if (selectedFiles.length > 0) {
        if (selectedFiles[0].getType() == VFSFile.DIRECTORY) {
          dir = selectedFiles[0].getPath();
        } else if (selectedFiles[0].getType() == VFSFile.FILE) {
          dir = MiscUtilities.getParentOfPath(selectedFiles[0].getPath());
        }
      }
      
      if (dir == null) {
        dir = browser.getDirectory();
      }
      // ディレクトリ名の後ろにセパレータをつける
      this.searchDirectory = MiscUtilities.getParentOfPath(MiscUtilities.constructPath(dir, "x"));
      files.clear();
      
      VFS vfs = VFSManager.getVFSForPath(searchDirectory);
      Object session = vfs.createVFSSession(searchDirectory, null);
      
      try {
        _listVFSFiles(vfs, session, searchDirectory, files);
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        try {
          vfs._endVFSSession(session, null);
        } catch (Exception e){
          e.printStackTrace();
        }
      }
    }
    
    public String getSearchDirectory() {
      return this.searchDirectory;
    }
    
    public String[] getFiles() {
      String[] stringFiles = null;
      synchronized (files) {
        stringFiles = new String[files.size()];
        for(int i = 0; i < stringFiles.length; i++) {
          stringFiles[i] = files.get(i);
        }
      }
      
      return stringFiles;
    }
    
    public int getFilesSize() {
      synchronized(files) {
        return files.size();
      }
    }
    
    public void stop() {
      this.stop = true;
    }
    
    private void _listVFSFiles(VFS vfs, Object session, String dir, Vector<String> files) throws Exception {
      if (this.stop) {
        return;
      }
      
      VFSFile[] list = vfs._listFiles(session, dir, null);
      
      for(VFSFile file: list) {
        if (this.stop) {
          return;
        }
        
        if (file.isHidden() || !file.isReadable()) {
          continue;
        }
        
        if (file.getType() == VFSFile.FILE) {
          synchronized (files) {
            files.add(file.getPath());
          }
        } else if (file.getType() == VFSFile.DIRECTORY) {
          _listVFSFiles(vfs, session, file.getPath(), files);
        }
      }
    }
  }
  
  static class RefreshRunnable implements Runnable {
    private boolean stop = false;
    private SwitchBufferDialogForDirectory dialog = null;
    private DirectorySearcher searcher = null;
    private int prevFilesSize = 0;
    
    public RefreshRunnable(SwitchBufferDialogForDirectory dialog, DirectorySearcher searcher) {
      this.dialog = dialog;
      this.searcher = searcher;
    }
    
    public void run() {
      while(!stop) {
        int filesSize = searcher.getFilesSize();
        if (prevFilesSize != filesSize) {
          dialog.refreshBufferList(dialog.bufferName.getText());
          prevFilesSize = filesSize;
        }
        
        try {
          Thread.sleep(500);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    
    public void stop() {
      this.stop = true;
    }
  }
}

