package at.ac.tuwien.dbai.hgtools;


import java.io.File;

import at.ac.tuwien.dbai.hgtools.csp2hg.HypergraphFromCSPHelper;
import at.ac.tuwien.dbai.hgtools.hypergraph.Hypergraph;



public class MainCSP {

	public static void main(String[] args) throws Exception {
				
		System.out.println("filename;vertices;edges;degree;bip;b3ip;b4ip;vc");
		
		for (int i=0; i < args.length; i++) {
			File file = new File(args[i]);
			File[] files;
			if (file.isDirectory()) {
				files = file.listFiles();
			} else {
				files = new File[1];
				files[0] = file;
			}
		    processFiles(files);
		}
	}
	
	public static void processFiles(File[] files) throws Exception {
	    for (File file : files) {
	        if (file.isDirectory()) {
	            //System.out.println("Directory: " + file.getName());
	            processFiles(file.listFiles()); // Calls same method again.
	        } else if (file.getName().contains("xml")) {
	    		HypergraphFromCSPHelper csp2hg = new HypergraphFromCSPHelper(file.getPath()); 
	    			
	    		Hypergraph H = csp2hg.getHypergraph();
	    			
	    		System.out.print(file.getPath()+";");
	    		System.out.print(H.cntVertices()+";");
	    		System.out.print(H.cntEdges()+";");
	    		System.out.print(H.degree()+";");
	    		System.out.print(H.cntBip(2)+";");
	    		System.out.print(H.cntBip(3)+";");
	    		System.out.print(H.cntBip(4)+";");
	    		System.out.println(H.VCdimension());
	        }
	    }
	}
	

	
}
