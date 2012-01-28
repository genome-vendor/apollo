/*
 * Created on Oct 8, 2004
 *
 */
package apollo.dataadapter;

import java.util.List;

import apollo.editor.TransactionManager;

/**
 * This class is used to translate Apollo Transaction objects (in package apollo.editor)
 that are in TransactionManager,
 * to another forms of Transaction object (e.g. chado).
 * @author wgm
 */
public interface TransactionTransformer {

  public List transform(TransactionManager apolloTnObjs)
    throws TransactionTransformException;
  
}
