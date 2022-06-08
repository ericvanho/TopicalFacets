package ericvh.TDT;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

/** ArrayListTransferHandler.java. Handles the transfer of data from one list 
 * to another and back after dragging and dropping a file.
 * DragListDemo.java from http://java.sun.com/docs/books/tutorial/uiswing/misc/dnd.html.
 * Created on 22 september 2005, 0:10
 */

public class ArrayListTransferHandler extends TransferHandler
{
    DataFlavor localArrayListFlavor, serialArrayListFlavor;
    String localArrayListType = DataFlavor.javaJVMLocalObjectMimeType +
            ";class=java.util.ArrayList";
    JList source = null;
    int[] indices = null;
    int addIndex = -1; //Location where items were added
    int addCount = 0;  //Number of items added
   Set mNegativeKeys = new HashSet();
    
    public ArrayListTransferHandler()
    {
        try
        {
            localArrayListFlavor = new DataFlavor(localArrayListType);
        }
        catch (ClassNotFoundException e)
        {
            System.out.println("ArrayListTransferHandler: unable to create data flavor");
        }
        serialArrayListFlavor = new DataFlavor(ArrayList.class, "ArrayList");
    }
    
    @Override
    public boolean importData(JComponent c, Transferable t)
    {
        JList target;
        ArrayList alist;
        if (!canImport(c, t.getTransferDataFlavors())) return false;
        try
        {
            target = (JList)c;
            if (hasLocalArrayListFlavor(t.getTransferDataFlavors()))
            {
                alist = (ArrayList)t.getTransferData(localArrayListFlavor);
            }
            else if (hasSerialArrayListFlavor(t.getTransferDataFlavors()))
            {
                alist = (ArrayList)t.getTransferData(serialArrayListFlavor);
            }
            else  return false;
        }
        catch (UnsupportedFlavorException ufe)
        {
            System.out.println("importData: unsupported data flavor");
            return false;
        }
        catch (IOException ioe)
        {
            System.out.println("importData: I/O exception");
            return false;
        }
        // At this point we use the same code to retrieve the data locally or serially.
        // We'll drop at the current selected index.
        int index = target.getSelectedIndex();
        // Prevent the user from dropping data back on itself.
        // For example, if the user is moving items #4,#5,#6 and #7 and attempts to insert the
        // items after item #5, this would be problematic when removing the original items.
        // This is interpreted as dropping the same data on itself and has no effect.
        if (source.equals(target))
        {
            if (indices != null && index >= indices[0] - 1 && index <= indices[indices.length - 1])
            {
                indices = null;
                return true;
            }
        }
        DefaultListModel listModel = (DefaultListModel)target.getModel();
        int max = listModel.getSize();
        if (index < 0) index = max;
        else
        {
            index++;
            if (index > max) index = max;
        }
        addIndex = index;
        addCount = alist.size();
        
        // Adaptation of the original source code to suit the purpose of the main TDT application.
        // Eric Van Horenbeeck 22 sept. 2005.
        for (int i=0; i < alist.size(); i++) 
        {
            String facet = alist.get(i).toString();
            listModel.add(index++, facet); 
            Integer negativeFacet;
            if(!facet.equals(""))
            {
                negativeFacet = Integer.parseInt(facet.substring(1, facet.indexOf(")")));
                if(mNegativeKeys.contains(negativeFacet)) mNegativeKeys.remove(negativeFacet);
                else mNegativeKeys.add(negativeFacet);
            }        
        }
        return true;
    }
    
    public Set getNegativeKeys()
    {
        return mNegativeKeys;
    }
    
    @Override
    protected void exportDone(JComponent c, Transferable data, int action)
    {
        if ((action == MOVE) && (indices != null))
        {
            DefaultListModel model = (DefaultListModel)source.getModel();   
            //If we are moving items around in the same list, we
            //need to adjust the indices accordingly since those
            //after the insertion point have moved.
            if (addCount > 0)
            {
                for (int i = 0; i < indices.length; i++)
                {
                    if (indices[i] > addIndex) indices[i] += addCount;
                }
            }
            for (int i = indices.length -1; i >= 0; i--)  model.remove(indices[i]);      
        }
        indices = null;
        addIndex = -1;
        addCount = 0;
    }
    
    private boolean hasLocalArrayListFlavor(DataFlavor[] flavors)
    {
        if (localArrayListFlavor == null) return false;
        for (DataFlavor flavor : flavors) {
            if (flavor.equals(localArrayListFlavor)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean hasSerialArrayListFlavor(DataFlavor[] flavors)
    {
        if (serialArrayListFlavor == null) return false;
        
        for (DataFlavor flavor : flavors) {
            if (flavor.equals(serialArrayListFlavor)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean canImport(JComponent c, DataFlavor[] flavors)
    {
        if (hasLocalArrayListFlavor(flavors))
        { return true; }
        return hasSerialArrayListFlavor(flavors);
    }
    
    @Override
    protected Transferable createTransferable(JComponent c)
    {
        if (c instanceof JList)
        {
            source = (JList)c;
            indices = source.getSelectedIndices();
            Object[] values = source.getSelectedValuesList().toArray();
            if (values == null || values.length == 0)  return null;
            ArrayList alist = new ArrayList(values.length);
            for (Object o : values) {
                String str = o.toString();
                if (str == null) str = "";
                alist.add(str);
            }      
            return new ArrayListTransferable(alist);
        }
        return null;
    }
    
    @Override
    public int getSourceActions(JComponent c)
    {
        return COPY_OR_MOVE;
    }
    
    public class ArrayListTransferable implements Transferable
    {
        ArrayList data;
        
        public ArrayListTransferable(ArrayList alist)
        {
            data = alist;
        }
        
        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
        {
            if (!isDataFlavorSupported(flavor))
            {
                throw new UnsupportedFlavorException(flavor);
            }
            return data;
        }
        
        @Override
        public DataFlavor[] getTransferDataFlavors()
        {
            return new DataFlavor[] { localArrayListFlavor, serialArrayListFlavor };
        }
        
        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor)
        {
            if (localArrayListFlavor.equals(flavor)) return true;
            return serialArrayListFlavor.equals(flavor);
        }
    }
    
}