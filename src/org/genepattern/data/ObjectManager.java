/*
 * ObjectManager.java
 *
 * Created on November 7, 2002, 4:03 PM
 */

package org.genepattern.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.ListModel;

/**
 * Class will manage all data objects that are available to GP where ever they are...
 *
 * @author  kohm
 */
public class ObjectManager {
    
    /** Creates a new instance of ObjectManager */
    protected ObjectManager() {
        data_map = new HashMap();
        model_map = new HashMap();
//        templates = new ArrayList(20);
//        datasets  = new ArrayList(20);
//        dataset_listmodel  = new ObjectListModel(datasets);
//        template_listmodel = new ObjectListModel(templates);
    }
    
    public static final ObjectManager instance() {
        return object_manager;
    }
    
    // "setters"
    /** adds a Dataset */
    public void add(final Dataset dataset) {
        final List datasets = getDataFor(Dataset.class);
        datasets.add(dataset);
    }
    /** add the object checking to make sure the object is an instanceof the class*/
    public final void add(final DataObjector data, final Class do_class) {
        if( !(data.getClass().isAssignableFrom(do_class)) )
            throw new IllegalArgumentException("Cannot add "+data+" as a "+do_class);
        List list = getDataFor(do_class);
        list.add(data);
    }
    // "getters"
    /** gets the ListModel that contains all the Datasets */
    public ListModel getDatasetListModel() {
        final ListModel dataset_listmodel = getListModelFor(Dataset.class);
        return dataset_listmodel;
    }
    /** returns a copy of the Datasets */
    public Dataset[] getAllDatasets() {
        final List datasets = getDataFor(Dataset.class);
        return (Dataset[])datasets.toArray(new Dataset[datasets.size()]);
    }
    
    /** gets the ListModel that contains all the Template objects */
    public ListModel getTemplateListModel() {
        final ListModel template_listmodel = getListModelFor(Template.class);
        return template_listmodel;
    }
    /** returns a copy of the Templates */
    public Template[] getAllTemplates() {
        final List templates = getDataFor(Template.class);
        return (Template[])templates.toArray(new Template[templates.size()]);
    }
    /** helper - gets the List of data associated with the class */
    private final List getDataFor(final Class data_class) {
        List list = (List)data_map.get(data_class);
        if(list == null) {
            createMapEntriesFor(data_class);
            list = (List)data_map.get(data_class);
        }
        return list;
    }
    /** gets the ListModel associated with the class */
    public final ListModel getListModelFor(final Class data_class) {
        ListModel model = (ListModel)model_map.get(data_class);
        if(model == null){
            createMapEntriesFor(data_class);
            model = (ListModel)model_map.get(data_class);
        }
        return model;
    }
    /** creates entries for the data_map and model_map */
    private final void createMapEntriesFor(final Class data_class) {
        if(data_class == null)
            throw new NullPointerException("Cannot create entries for a null data class");
        final List data = new ArrayList();
        if(data_map.containsKey(data_class) || model_map.containsKey(data_class)) {
            throw new IllegalArgumentException("Already have a map entry for a data class of type "+data_class+"!");
        }
        data_map.put(data_class, data);
        model_map.put(data_class, new ObjectManager.ObjectListModel(data));
    }
    /**
     * checks
     * the current list of Dataset objects for 
     * the names
     */
    public final String getUniqueName(final String proposed) {
        String label = proposed;
        for(int i = 0; !isNameUnique(label); i++) {
            label = proposed + "_"+i;
        }
        return label; 
    }
    /**
     * FIXME this may not be unique need to check
     * the current list of all objects for 
     * the names
     */
    public final String getUniqueDatasetName(final String proposed) {
        //NOT full implemented
        return proposed+"_2";
    }
    /** checks if the name is unique amonst all data objects */
    public final boolean isNameUnique(final String proposed) {
        for(Iterator iter = data_map.keySet().iterator(); iter.hasNext(); ) {
            final Class dclass = (Class)iter.next();
            if( !isNameUniqueFor(dclass, proposed) )
                return false;
        }
        return true;
    }
    /** checks if the name is unique for the specified class */
    public final boolean isNameUniqueFor(final Class key, final String proposed) {
        final List datas = (List)data_map.get(key);
        if(datas == null)
            return true;
        final int limit = datas.size();
        for(int i = 0; i < limit; i++) {
            String old_name = ((DataObjector)datas.get(i)).getName();
            if(proposed.equals(old_name)) {
                return false;  
            }
        }
        return true;
    }
    
    //fields
    // FIXME THIS ISonly here for UISomViewFrame
        // Constants: same order as ObjectTypeNames and objects in fAllObjectList
    public static final int kDataset = 0;
    public static final int kClassAssignments = 1;
    public static final int kPredictorAlg = 2;
    public static final int kPredictorResults = 3;
    public static final int kSomAlg = 4;
    public static final int kFeatureSummary = 5;
    public static final int kFilterScheme = 6;
    public static final int kFeatureSet = 7;
    public static final int kClusterViews = 8;
    public static final int kHistogramViews = 9;
    public static final int kGeneList = 10;
    public static final int kScatterAlg = 11;
    public static final int kPermStats = 12;
    public static final int kPredictorValAlg = 13;
    /** the object was deleted from the its list of objects */
    public static final int kObjectDeleted = 128;
    /** a list is now empty  all object have been deleted */
    public static final int EMPTY_LIST = 129;
    /** a list now has its' first item added */
    public static final int FIRST_ADDED = 256;

    static final String kRandomAssignmentsStr = "Random Assignments";
    
    /** the instance of this class */
    private static final ObjectManager object_manager = new ObjectManager();
//    /** the model that holds all the known datasets */
//    private final ObjectListModel dataset_listmodel;
//    /** where the Dataset objects are stored */
//    private final List datasets;
//    /** the model that holds all the known Template objects */
//    private final ObjectListModel template_listmodel;
//    /** where the Template objects are stored */
//    private final List templates;
    /** contains all the List objects of data */
    private final Map data_map;
    /** contains all the ListModel objects for viewing the data as a list of some sort */
    private final Map model_map;
    
    
    // I N N E R   C L A S S E S
    /** "immutable" ListModel */
    public static class ObjectListModel extends javax.swing.AbstractListModel {
        
        /** Creates a new instance of ObjectListModel */
        ObjectListModel(final List objects) {
            this.objects = objects;
        }
        
        /** Returns the value at the specified index.
         * @param index the requested index
         * @return the value at <code>index</code>
         *
         */
        public Object getElementAt(final int index) {
            return objects.get(index);
        }
        
        /**
         * Returns the length of the list.
         * @return the length of the list
         *
         */
        public int getSize() {
            return objects.size();
        }
        
        // fields
        
        /** keeps the objects */
        private final List objects;
        
    } //end ObjectListModel
    
    /**
     * DefaultComboBoxModel.java
     *
     */
    
    static class SimpleComboBoxModel extends ObjectListModel implements javax.swing.ComboBoxModel {
        
        /**
         * Constructs an empty DefaultComboBoxModel object.
         */
        public SimpleComboBoxModel(List list) {
            super(list);
        }
        
        // implements javax.swing.ComboBoxModel
        /**
         * Set the value of the selected item. The selected item may be null.
         * <p>
         * @param anObject The combo box value or null for no selection.
         */
        public void setSelectedItem(Object anObject) {
            if ((selectedObject != null && !selectedObject.equals( anObject )) ||
            selectedObject == null && anObject != null) {
                selectedObject = anObject;
                fireContentsChanged(this, -1, -1);
            }
        }
        
        // implements javax.swing.ComboBoxModel
        public Object getSelectedItem() {
            return selectedObject;
        }
        
        //fields 
        /** the currently selected object */
        protected Object selectedObject;
        
    }// end SimpleComboBoxModel

}
