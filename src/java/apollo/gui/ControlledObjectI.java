package apollo.gui;

/**
 * Interface defining methods required to add a Controller to an object
 getControllerWindow and needs AutoRemoval used by Controller to remove listeners
 when their controller window is closing.
 used by ContainerView to propigate controller to views that are ControlledObjects
 same with ApolloPanel and its views.
 Also used by SyntenyLinkPanel to avoid firing at ControlledObjectI event source
 why does it implement Serialazable? im not necasarily opposed - just curious - MG
 */

public interface ControlledObjectI extends java.io.Serializable {
  /**
   * Sets the Controller for the object
   */
  public void setController(Controller controller);
  /**
   * Gets the Controller for the object
   */
  public Controller getController();
  /** Shouldnt this return Window - would we ever want a non-window? */
  public Object     getControllerWindow();
  /** If getControllerWindow is non null, and needsAutoRemoval is true then
      Controller will automatically remove the ControlledObjectI as a listener
      when its ControllerWindow is closing. If getControllerWindow is null 
      needsAutoRemoval is meaningless. Rename this removeAsListenerOnWindowClose? */
  public boolean    needsAutoRemoval();
}
