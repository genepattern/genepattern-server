package org.genepattern.server.webapp.rest.api.v1.job.pipeline;

import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.executor.pipeline.PipelineGraph;
import org.genepattern.server.executor.pipeline.PipelineGraph.MyEdge;
import org.genepattern.server.executor.pipeline.PipelineGraph.MyVertex;
import org.genepattern.server.webapp.rest.api.v1.job.GetJobLegacy;
import org.genepattern.webservice.JobInfo;
import org.jgrapht.Graph;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.traverse.GraphIterator;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Utility class for iterating the tree of jobs for a pipeline, so that we can generate a JSON 
 * representation.
 * 
 * @author pcarr
 *
 */
public class JobInfoVisitorUtil {
    private static final Logger log = Logger.getLogger(JobInfoVisitorUtil.class);
    
    public static final class InitChildJobs extends TraversalListenerAdapter<MyVertex,MyEdge> {
        private final Context userContext;
        private final SortedMap<Integer, JSONObject> steps=new TreeMap<Integer,JSONObject>();
        public JSONArray getChildren() {
            final JSONArray children=new JSONArray();
            for(final JSONObject step : steps.values()) {
                children.put(step);
            }
            return children;
        }
        
        public InitChildJobs(final Context userContext) {
            this.userContext=userContext;
        }

        @Override
        public void vertexTraversed(VertexTraversalEvent<MyVertex> arg0) {
            log.debug("vertexTraversed");
            final boolean includeChildren=false;
            String jobId="UNKNOWN";
            try {
                final MyVertex vertex=arg0.getVertex();
                jobId=""+vertex.getJobInfo().getJobNumber();
                final GetJobLegacy getJobImpl = new GetJobLegacy();
                JSONObject job=getJobImpl.getJob(userContext, jobId, includeChildren);
                final Integer stepId=Integer.parseInt( vertex.getStepId() );
                job.put("stepId", stepId);
                steps.put(stepId, job);
            }
            //catch (GetJobException e) {
            //}
            //catch (JSONException e) {
            //}
            catch (Throwable t) {
                log.error("Error getting info for jobId="+jobId, t);
            }
        }
    }

    static public void test(final JobInfo pipelineJobInfo) {
        walkPipelineGraph(new TraversalListener<MyVertex,MyEdge>() {

            @Override
            public void connectedComponentFinished(ConnectedComponentTraversalEvent arg0) {
                // TODO Auto-generated method stub
                log.debug("connectedComponentFinished");
            }

            @Override
            public void connectedComponentStarted(ConnectedComponentTraversalEvent arg0) {
                // TODO Auto-generated method stub
                log.debug("connectedComponentStarted");
                
            }

            @Override
            public void edgeTraversed(EdgeTraversalEvent<MyVertex,MyEdge> arg0) {
                // TODO Auto-generated method stub
                log.debug("edgeTraversed");
            }

            @Override
            public void vertexFinished(VertexTraversalEvent<MyVertex> arg0) {
                // TODO Auto-generated method stub
                log.debug("vertexFinished");
            }

            @Override
            public void vertexTraversed(VertexTraversalEvent<MyVertex> arg0) {
                // TODO Auto-generated method stub
                log.debug("vertexTraversed");
            }
        },
        pipelineJobInfo);
    }
    
    public static void walkPipelineGraph(final TraversalListener<MyVertex,MyEdge> listener, final JobInfo pipelineJobInfo) {
        if (pipelineJobInfo == null) {
            log.error("pipelineJobInfo is null");
            return;
        }
        PipelineGraph graph = PipelineGraph.getDependencyGraph(pipelineJobInfo);
        Graph<MyVertex,MyEdge> jobGraph = graph.getGraph();
        GraphIterator<MyVertex,MyEdge> iter = new DepthFirstIterator<MyVertex,MyEdge>(jobGraph);
        iter.addTraversalListener(listener);
        while(iter.hasNext()) {
            final MyVertex next=iter.next();
            log.debug("nextVertex, stepId="+next.getStepId()+", jobId="+next.getJobInfo().getJobNumber());
        }        
    }
    
}
