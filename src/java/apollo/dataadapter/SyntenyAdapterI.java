package apollo.dataadapter;

import apollo.datamodel.*;
import java.util.*;

public interface SyntenyAdapterI {
  public String getType();
  public void setType(String type);

  public String getName(); 
  public void setName(String name); 

  public KaryotypeAdapterI getKaryotypeAdapter();

  public Vector getSyntenyRegionsByChromosome(Chromosome chr);
  public void addSyntenyRegionForChromosome(String chr, SyntenyRegion region);
}
