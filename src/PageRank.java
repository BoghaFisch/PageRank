/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2012
 */  

import java.util.*;
import java.io.*;

public class PageRank{

    /**  
     *   Maximal number of documents. We're assuming here that we
     *   don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

    /**
     *   Mapping from document names to document numbers.
     */
    Hashtable<String,Integer> docNumber = new Hashtable<String,Integer>();

    /**
     *   Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**  
     *   A memory-efficient representation of the transition matrix.
     *   The outlinks are represented as a Hashtable, whose keys are 
     *   the numbers of the documents linked from.<p>
     *
     *   The value corresponding to key i is a Hashtable whose keys are 
     *   all the numbers of documents j that i links to.<p>
     *
     *   If there are no outlinks from i, then the value corresponding 
     *   key i is null.
     */
    Hashtable<Integer,Hashtable<Integer,Boolean>> link = new Hashtable<Integer,Hashtable<Integer,Boolean>>();

    /**
     *   The number of outlinks from each node.
     */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

    /**
     *   The number of documents with no outlinks.
     */
    int numberOfSinks = 0;

    /**
     *   The probability that the surfer will be bored, stop
     *   following links, and take a random jump somewhere.
     */
    final static double BORED = 0.15;

    /**
     *   Convergence criterion: Transition probabilities do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.0001;

    /**
     *   Never do more than this number of iterations regardless
     *   of whether the transistion probabilities converge or not.
     */
    final static int MAX_NUMBER_OF_ITERATIONS = 1000; 

    
    /* --------------------------------------------- */


    public PageRank( String filename ) {
    	int noOfDocs = readDocs( filename );
    	computePagerank( noOfDocs );
    }


    /* --------------------------------------------- */


    /**
     *   Reads the documents and creates the docs table. When this method 
     *   finishes executing then the @code{out} vector of outlinks is 
     *   initialised for each doc, and the @code{p} matrix is filled with
     *   zeroes (that indicate direct links) and NO_LINK (if there is no
     *   direct link. <p>
     *
     *   @return the number of documents read.
     */
    int readDocs( String filename ) {
		int fileIndex = 0;
		try {
		    System.err.print( "Reading file... " );
		    BufferedReader in = new BufferedReader( new FileReader( filename ));
		    String line;
		    while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
				int index = line.indexOf( ";" );
				String title = line.substring( 0, index );
				Integer fromdoc = docNumber.get( title );
				//  Have we seen this document before?
				if ( fromdoc == null ) {
				    // This is a previously unseen doc, so add it to the table.
				    fromdoc = fileIndex++;
				    docNumber.put( title, fromdoc );
				    docName[fromdoc] = title;
				}
				// Check all outlinks.
				StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
				while ( tok.hasMoreTokens() && fileIndex<MAX_NUMBER_OF_DOCS ) {
				    String otherTitle = tok.nextToken();
				    Integer otherDoc = docNumber.get( otherTitle );
				    if ( otherDoc == null ) {
						// This is a previousy unseen doc, so add it to the table.
						otherDoc = fileIndex++;
						docNumber.put( otherTitle, otherDoc );
						docName[otherDoc] = otherTitle;
				    }
				    // Set the probability to 0 for now, to indicate that there is
				    // a link from fromdoc to otherDoc.
				    if ( link.get(fromdoc) == null ) {
				    	link.put(fromdoc, new Hashtable<Integer,Boolean>());
				    }
				    if ( link.get(fromdoc).get(otherDoc) == null ) {
				    	link.get(fromdoc).put( otherDoc, true );
				    	out[fromdoc]++;
				    }
				}
		    }
		    if ( fileIndex >= MAX_NUMBER_OF_DOCS ) {
		    	System.err.print( "stopped reading since documents table is full. " );
		    }
		    else {
		    	System.err.print( "done. " );
		    }
		    // Compute the number of sinks.
		    for ( int i=0; i<fileIndex; i++ ) {
		    	if ( out[i] == 0 )
		    		numberOfSinks++;
		    }
		}
		catch ( FileNotFoundException e ) {
		    System.err.println( "File " + filename + " not found!" );
		}
		catch ( IOException e ) {
		    System.err.println( "Error reading file " + filename );
		}
		System.err.println( "Read " + fileIndex + " number of documents" );
		return fileIndex;
    }


    /* --------------------------------------------- */


    /*
     *   Computes the pagerank of each document.
     */
    void computePagerank( int numberOfDocs ) {
    	int N = numberOfDocs;
    	double[] pi = powerIteration(N);
    	Integer[] docIDs = new Integer[N];
    	
    	// Create array of document ids
    	for (int i = 0; i < N; i++) {
    		docIDs[i] = i;
    	}
    	
    	// Sort the array by the array pi
        Arrays.sort(docIDs, new Comparator<Integer>() {
            @Override
            public int compare(Integer i1, Integer i2) {
            	
                return -1*Double.compare(pi[i1],pi[i2]);
            }
        });
    	for (int i = 0; i < 50; i++) {
    		if (i < pi.length)
    			System.out.println(docName[docIDs[i]] + " " + pi[docIDs[i]]);
    	}
    	
    }
    private double[] powerIteration( int N ) {
    	double[] pi = new double[N];
    	
    	// Initialize to 0 for all except first
    	pi[0] = 1.0;
    	for (int i = 1; i < N; i++)
    		pi[i] = 0.0;
    	
    	
    	boolean keepGoing = true;
    	int iterations = 0;
    	long startTime = System.currentTimeMillis();
    	while (keepGoing && iterations < MAX_NUMBER_OF_ITERATIONS) {
    		double[] next = new double[N];
    	    for ( int i = 0 ; i < N ; i++ ) {
    	    	for ( int j = 0 ; j < N ; j++ ) {
    	    		
    	    		// Calculate the probability of a transition (point P[i][j] in the transition matrix)
    				double pij = BORED/N;
    				
    				// If the document contains no outlinks, move with equal probability to every other document
    				if (out[i] == 0) {
    					pij += (1-BORED)/N;
    				}
    				// Else if doc i has a link to doc j split between all outlinks
    				else if (link.get(i).get(j) != null) {
    					pij += (1-BORED)/out[i];
    				}
    				
    				next[j] += pi[i]*pij;
    			}
    		}
	    	
    	    keepGoing = false;
	    	// Check if we are done
	    	for (int i = 0; i < N; i++) {
	    		if ( Math.abs(pi[i] - next[i]) > EPSILON)
	    			keepGoing = true;
	    	}
	    	// Update pi with the new values
	    	for (int i = 0; i < N; i++) {
	    		pi[i] = next[i];
	    	}
	    	iterations++;
    	}
    	System.out.println("Time taken:" + (System.currentTimeMillis() - startTime));
    	
    	return pi;
    }

    /* --------------------------------------------- */


    public static void main( String[] args ) {
	/*if ( args.length != 1 ) {
	    System.err.println( "Please give the name of the link file" );
	}
	else {
	    new PageRank( args[0] );
	}*/
    	new PageRank( "linksDavis.txt");
    }
}
