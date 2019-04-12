/*
 
 
 */

package motiflab.engine;

/**
 *
 * @author kjetikl
 */
public class GeneIdentifier {
        public String identifier;
        public String format;
        public int organism;   
        public String build;
        
        public GeneIdentifier(String identifier, String format, int organism, String build) {
            this.identifier=identifier;
            this.format=format;
            this.organism=organism;
            this.build=build;
        }
}
