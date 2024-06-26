/* {{{ header
* :tabSize=4:indentSize=4:noTabs=false:folding=explicit:collapseFolds=1:
*
* SwitchBufferPlugin.java - Main class for the SwitchBuffer plugin
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
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.LinkedHashSet;
import java.util.Iterator;
import javax.swing.JDialog;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EBPlugin;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.OptionsDialog;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.msg.ViewUpdate;
import org.gjt.sp.jedit.msg.BufferChanging;
import org.gjt.sp.util.Log;
//}}}

/**
* A plugin for <a href="http://www.jedit.org/">jEdit</a> providing a quick and
* easy way to change buffers.
*
* @author    <a href="mailto:lee@leeturner.org">Lee Turner</a>
* @version   $Revision: 1.10 $ $Date: 2003/10/28 09:39:38 $
*/
public class SwitchBufferPlugin extends EBPlugin
{
	//{{{ static fields
	private static Map dialogsMap = new HashMap();
	//}}}
	
	// private static LinkedHashSet recentBuffers = new LinkedHashSet();
	private static HashMap<EditPane,LinkedHashSet> editPaneMap = new HashMap<EditPane,LinkedHashSet>();
	
	//{{{ instance fields
	private String currentFile = "";
	//}}}
	
	//{{{ +SwitchBufferPlugin() : <init>
	/**
	* Default Constructor for the <tt>SwitchBufferPlugin</tt> object
	*/
	public SwitchBufferPlugin() { }//}}}
	
	static Buffer[] getBuffersByRecentOrder(){
		if (!jEdit.getBooleanProperty("switchbuffer.options.recent-order",false)){
			return jEdit.getActiveView().getEditPane().getBufferSet().getAllBuffers();
		}
		
		LinkedHashSet buffers = new LinkedHashSet();
		// Buffer[] array = jEdit.getBuffers();
		Buffer[] array = jEdit.getActiveView().getEditPane().getBufferSet().getAllBuffers();
		for (int i = array.length - 1; i >= 0; i--){
			buffers.add(array[i]);
		}
		
		Iterator it = editPaneMap.get(jEdit.getActiveView().getEditPane()).iterator();
		while(it.hasNext()){
			Buffer buffer = (Buffer)(it.next());
			if (buffers.contains(buffer)){
				buffers.remove(buffer);
				buffers.add(buffer);
			}
		}
		// Order Reverse
		Buffer[] result = new Buffer[buffers.size()];
		buffers.toArray(result);
		for (int i = 0; i < result.length / 2; i++){
			Buffer back = result[i];
			int left = i;
			int right = result.length - 1 - i;
			result[left] = result[right];
			result[right] = back;
		}
		return result;
	}
	
	//{{{ +handleMessage(EBMessage) : void
	/**
	* Handles the messages on the jEdit message bus that SwitchBuffer
	* is interested in
	*
	* @param message  The message from the message bus.
	*/
	public void handleMessage(EBMessage message)
	{
		if (message instanceof BufferChanging){
			Buffer buffer = ((BufferChanging)message).getBuffer();
			EditPane ep = ((BufferChanging)message).getEditPane();
			// System.out.println("Buffer Changing " + buffer);
			LinkedHashSet hashset = editPaneMap.get(ep);
			if (hashset != null){
				hashset.remove(buffer);
				hashset.add(buffer);
			}
		}
		boolean rememberPreviousFromAnywhere = jEdit.getBooleanProperty("switchbuffer.options.remember-previous-buffer-from-anywhere");
		
		if(message instanceof EditPaneUpdate)
		{
			EditPaneUpdate epu = (EditPaneUpdate)message;
			if(epu.getWhat() == EditPaneUpdate.BUFFER_CHANGED && rememberPreviousFromAnywhere)
			{
				jEdit.setTemporaryProperty("switchbuffer.last-open-file", currentFile);
				Buffer buf = epu.getEditPane().getBuffer();
				currentFile = buf.getName();
			}
			
			if (epu.getWhat() == EditPaneUpdate.CREATED){
				if (!editPaneMap.containsKey(epu.getEditPane())){
					editPaneMap.put(epu.getEditPane(), new LinkedHashSet());
				}
			} else if (epu.getWhat() == EditPaneUpdate.DESTROYED){
				editPaneMap.remove(epu.getEditPane());
			}
		}
		
		if(message instanceof BufferUpdate)
		{
			BufferUpdate bu = (BufferUpdate)message;
			if(bu.getWhat() == BufferUpdate.SAVED)
			{
				Buffer buf = bu.getView().getBuffer();
				currentFile = buf.getName();
			}
			if(bu.getWhat() == BufferUpdate.CLOSED)
			{
				Buffer buf = bu.getBuffer();
				Iterator<LinkedHashSet> it = editPaneMap.values().iterator();
				while(it.hasNext()){
					it.next().remove(buf);
				}
				
				if(buf.getName().equals(currentFile))
				{
					currentFile = "";
				}
			}
			if(bu.getWhat() == BufferUpdate.PROPERTIES_CHANGED)
			{
				// clear out the current cache of colours.
				SwitchBufferUtils.clearColourCache();
			}
		}
		
		if(message instanceof ViewUpdate)
		{
			ViewUpdate vu = (ViewUpdate)message;
			if(vu.getWhat() == ViewUpdate.CREATED)
			{
				Buffer buf = vu.getView().getBuffer();
				currentFile = buf.getName();
			}
		}
		
	}//}}}
	
	//{{{ +switchBuffer(View) : void
	public static void switchBuffer(View view, boolean orderByRecent){
		jEdit.setBooleanProperty("switchbuffer.options.recent-order",orderByRecent);
		switchBuffer(view);
	}
	
	/**
	* Loads and diaplays the SwitchBuffer dialog while saving the parent view for
	* future use.
	*
	* @param view  The parent jEdit view
	*/
	public static void switchBuffer(View view)
	{
		if(view == null)
		{
			return;
		}
		Object obj = null;
		if(!dialogsMap.containsKey(view))
		{
			obj = new SwitchBufferDialog(view);
			dialogsMap.put(view, obj);
		}
		else
		{
			obj = (JDialog)dialogsMap.get(view);
		}
		
		jEdit.setTemporaryProperty("switchbuffer.file-suffix-switch.filename", "");
		
		GUIUtilities.loadGeometry(((java.awt.Window)(obj)), "switchbuffer.dialog");
		((JDialog)(obj)).setVisible(true);
	}
	//}}}
	
	//{{{ +fileSuffixSwitch(Buffer, View) : void
	public static void fileSuffixSwitch(Buffer buffer, View view)
	{
		if(view == null)
		{
			return;
		}
		if(buffer == null)
		{
			return;
		}
		
		boolean flag = jEdit.getBooleanProperty("switchbuffer.options.ignore-case");
		String currentBufferName = buffer.getName();
	currentBufferName = flag ? currentBufferName.toLowerCase() : currentBufferName;
		String textToMatch = null;
		if(currentBufferName.lastIndexOf('.') != -1)
		{
			textToMatch = currentBufferName.substring(0, currentBufferName.lastIndexOf('.')+1);
		}
		
		Buffer buffers[] = SwitchBufferPlugin.getBuffersByRecentOrder();
		Vector vector = new Vector(buffers.length);
		for(int i = 0; i < buffers.length; i++)
		{
		String bufferName = flag ? buffers[i].getName().toLowerCase() : buffers[i].getName();
			if((bufferName.startsWith(textToMatch)) && !(bufferName.equals(currentBufferName)))
			{
				vector.add(buffers[i]);
			}
		}
		
		if(jEdit.getBooleanProperty("switchbuffer.options.file-suffix-switch.auto-switch") == true && vector.size() == 1)
		{
			view.setBuffer((Buffer)vector.get(0));
		}
		else
		{
			Object obj = null;
			if(!dialogsMap.containsKey(view))
			{
				obj = new SwitchBufferDialog(view);
				dialogsMap.put(view, obj);
			}
			else
			{
				obj = (JDialog)dialogsMap.get(view);
			}
			
			jEdit.setTemporaryProperty("switchbuffer.file-suffix-switch.filename", textToMatch);
			
			GUIUtilities.loadGeometry(((java.awt.Window)(obj)), "switchbuffer.dialog");
			((JDialog)(obj)).setVisible(true);
		}
	} //}}}
}

