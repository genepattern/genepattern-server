package org.genepattern.gpge.ui.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.AnalysisService;

/**
 * Utility class for organizing information about analysis services
 * 
 * @author Joshua Gould
 */
public class AnalysisServiceUtil {
   /** Case insensitive comparator used to sort analysis services */
   public static final Comparator CASE_INSENSITIVE_TASK_NAME_COMPARATOR;
   
	private AnalysisServiceUtil() {
	}
   
   static {
      CASE_INSENSITIVE_TASK_NAME_COMPARATOR = new Comparator() {
         public int compare(Object obj1, Object obj2) {
            AnalysisService svc1 = (AnalysisService) obj1;
            AnalysisService svc2 = (AnalysisService) obj2;
            return svc1.getTaskInfo().getName().toLowerCase().compareTo(
                  svc2.getTaskInfo().getName().toLowerCase());
         }
      };
   }

	/**
	 * Sorts the map of categories to analysis services as returned from
	 * getCategoryToAnalysisServicesMap
	 * 
	 * @param categoryToAnalysisServicesMap
	 *            Description of the Parameter
	 */
	private static void sortAnalysisServices(Map categoryToAnalysisServicesMap) {
		for (Iterator values = categoryToAnalysisServicesMap.values()
				.iterator(); values.hasNext();) {
			List services = (List) values.next();
			java.util.Collections.sort(services, CASE_INSENSITIVE_TASK_NAME_COMPARATOR);

		}
	}

	/**
	 * Creates a map of task categories to a sorted list of analysis services
	 * for a category
	 * 
	 * @param latestServices
	 *            Description of the Parameter
	 * @return the map
	 */

	public static Map getCategoryToAnalysisServicesMap(Collection latestServices) {
		Map categories2Tasks = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		for (Iterator it = latestServices.iterator(); it.hasNext();) {
			AnalysisService svc = (AnalysisService) it.next();
			String category = (String) svc.getTaskInfo()
					.getTaskInfoAttributes().get(GPConstants.TASK_TYPE);
         if(category!=null && !category.equals("")) {
            category = Character.toUpperCase(category.charAt(0))
					+ category.substring(1, category.length());
         } else {
            category = "Uncategorized";  
         }
			List services = (List) categories2Tasks.get(category);
			if (services == null) {
				services = new ArrayList();
				categories2Tasks.put(category, services);
			}
			services.add(svc);
		}
		sortAnalysisServices(categories2Tasks);
		return categories2Tasks;
	}
}