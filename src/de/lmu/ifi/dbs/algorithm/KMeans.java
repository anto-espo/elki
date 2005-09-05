package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.Clusters;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.data.FeatureVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.normalization.AttributeWiseDoubleVectorNormalization;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Provides the k-means algorithm.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class KMeans<T extends FeatureVector> extends DistanceBasedAlgorithm<T>
{
    /**
     * Parameter k.
     */
    public static final String K_P = "k";
    
    /**
     * Description for parameter k.
     */
    public static final String K_D = "<int>k - the number of clusters to find (positive integer)";

    /**
     * Keeps k - the number of clusters to find.
     */
    private int k;
    
    /**
     * Keeps the result.
     */
    private Clusters result;
    
    /**
     * Provides the k-means algorithm.
     */
    public KMeans()
    {
        super();
        parameterToDescription.put(K_P+OptionHandler.EXPECTS_VALUE,K_D);
        optionHandler = new OptionHandler(parameterToDescription,KMeans.class.getName());
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
     */
    public Description getDescription()
    {
        // TODO reference
        return new Description("K-Means", "K-Means", "finds a partitioning into k clusters", "...");
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getResult()
     */
    public Result getResult()
    {
        return result;
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#run(de.lmu.ifi.dbs.database.Database)
     */
    public void run(Database<T> database) throws IllegalStateException
    {
        Random random = new Random();
        if(database.size() > 0)
        {
            T randomBase = database.get(database.iterator().next());
            AttributeWiseDoubleVectorNormalization normalization = new AttributeWiseDoubleVectorNormalization();
            if(randomBase instanceof DoubleVector)
            {
                // TODO generalize ?!?
                List<DoubleVector> list = new ArrayList<DoubleVector>(database.size());
                for(Iterator<Integer> dbIter = database.iterator(); dbIter.hasNext();)
                {
                    list.add((DoubleVector) database.get(dbIter.next()));
                }
                try
                {
                    normalization.normalize(list);
                }
                catch(NonNumericFeaturesException e)
                {
                    e.printStackTrace();
                }
            }
            List<T> means = new ArrayList<T>(k);
            List<T> oldMeans;
            List<List<Integer>> clusters = new ArrayList<List<Integer>>(k);
            if(isVerbose())
            {
                System.out.println("initializing random vectors");
            }
            for(int i = 0; i < k; i++)
            {
                T randomVector = (T) randomBase.randomInstance(random);
                if(randomVector instanceof DoubleVector)
                {
                    try
                    {
                        means.add((T) normalization.restore((DoubleVector) randomVector));
                    }
                    catch(NonNumericFeaturesException e)
                    {
                        e.printStackTrace();
                        means.add(randomVector);
                    }
                }
                else
                {
                    means.add(randomVector);
                }
            }
            clusters = sort(means, database);
            boolean changed = true;
            int iteration = 1;
            while(changed)
            {
                if(isVerbose())
                {
                    System.out.println("iteration "+iteration);
                }
                oldMeans = new ArrayList<T>(k);
                oldMeans.addAll(means);
                means = means(clusters, means, database);
                clusters = sort(means, database);
                changed = !means.equals(oldMeans);
                iteration++;
            }
            Integer[][] resultClusters = new Integer[clusters.size()][];
            for(int i = 0; i < clusters.size(); i++)
            {
                List<Integer> cluster = clusters.get(i);
                resultClusters[i] = cluster.toArray(new Integer[cluster.size()]);
            }
            result = new Clusters<T>(resultClusters,database);
        }
        else
        {
            result = new Clusters<T>(new Integer[0][0], database);
        }
    }
    
    /**
     * Returns the mean vectors of the given clusters in the given database.
     * 
     * 
     * @param clusters the clusters to compute the means
     * @param means the recent means
     * @param database the database containing the vectors
     * @return the mean vectors of the given clusters in the given database
     */
    protected List<T> means(List<List<Integer>> clusters, List<T> means, Database<T> database)
    {
        List<T> newMeans = new ArrayList<T>(k);
        for(int i = 0; i < k; i++)
        {
            List<Integer> list = clusters.get(i);
            T mean = null;
            for(Iterator<Integer> clusterIter = list.iterator(); clusterIter.hasNext();)
            {
                if(mean == null)
                {
                    mean = database.get(clusterIter.next());
                }
                else
                {
                    mean = (T) mean.plus(database.get(clusterIter.next()));
                }
            }
            if(list.size() > 0)
            {
                mean = (T) mean.multiplicate(1.0 / list.size());
            }
            else // mean == null
            {
                mean = means.get(i);
            }
            newMeans.add(mean);
        }
        return newMeans;
    }

    /**
     * Returns a list of clusters. The k<sup>th</sup> cluster contains the ids
     * of those FeatureVectors, that are nearest to the
     * k<sup>th</sup> mean.
     * 
     * 
     * @param means a list of k means
     * @param database the database to cluster
     * @return list of k clusters
     */
    protected List<List<Integer>> sort(List<T> means, Database<T> database)
    {
        List<List<Integer>> clusters = new ArrayList<List<Integer>>(k);
        for(int i = 0; i < k; i++)
        {
            clusters.add(new ArrayList<Integer>());
        }
        for(Iterator<Integer> dbIter = database.iterator(); dbIter.hasNext();)
        {
            Distance[] distances = new Distance[k];
            Integer id = dbIter.next();
            T fv = database.get(id);
            int minIndex = 0;
            for(int d = 0; d < k; d++)
            {
                distances[d] = getDistanceFunction().distance(fv, means.get(d));
                if(distances[d].compareTo(distances[minIndex]) < 0)
                {
                    minIndex = d;
                }
            }
            clusters.get(minIndex).add(id);
        }
        for(Iterator<List<Integer>> clustersIter = clusters.iterator(); clustersIter.hasNext();)
        {
            Collections.sort(clustersIter.next());
        }
        return clusters;
    }
    
    /**
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
     */
    @Override
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingParameters = super.setParameters(args);
        try
        {
            k = Integer.parseInt(optionHandler.getOptionValue(K_P));
            if(k <= 0)
            {
                throw new IllegalArgumentException("Parameter "+K_P+" must be a positive integer.");
            }
        }
        catch(NumberFormatException e)
        {
            throw new IllegalArgumentException(e);
        }
        catch(UnusedParameterException e)
        {
            throw new IllegalArgumentException(e);
        }
        return remainingParameters;
    }
    
}
