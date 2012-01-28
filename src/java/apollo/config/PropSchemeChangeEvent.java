package apollo.config;

import java.util.EventObject;

/** Should this be called TierChangeEvent or TypeChangeEvent - it fires for just
 tiers changing. However the granularity of information is the whole prop scheme - 
 all types - so even if only a single tier has changed this event doesnt convey
 such detail. It could even be called PropertySchemeChangeEvent, as all you know is 
 that the PropertyScheme has changed. Should it carry more info about tier or type 
 or is that not necasary? FeatureTierManager and TypePanel are the main 2 consumers 
 of this event, both of which go through the whole prop scheme. If this event ever 
 does convey the details of the change, it should be renamed, but i dont think this
 will happen as it doesnt seem necasary. */
public class PropSchemeChangeEvent extends EventObject {
  private PropertyScheme properties;

  public PropSchemeChangeEvent(Object source, PropertyScheme properties) {
    super(source);
    this.properties = properties;
  }

  public PropertyScheme getPropertyScheme() {
    return properties;
  }
  public Object getSource() {
    return source;
  }
}


