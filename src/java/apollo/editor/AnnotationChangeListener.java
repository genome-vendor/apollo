package apollo.editor;

import java.util.EventListener;

public interface AnnotationChangeListener extends EventListener {

  public boolean handleAnnotationChangeEvent(AnnotationChangeEvent evt);

  //public boolean handleAnnotationDeleteEvent(AnnotationDeleteEvent e); ??

}


