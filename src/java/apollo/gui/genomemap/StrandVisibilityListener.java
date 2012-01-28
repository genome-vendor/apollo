package apollo.gui.genomemap;

/** At the moment only for becoming visible, if need event for hiding as well
    then add another method (or variable) 
    This is presently pase' and not used - was used by ViewMenu, but 
    ViewMenu.ViewMenuListener now does the same thing in an easier fashion.
    This should probably be cvs removed. My only hesitation is this might come in
    handy with synteny stuff - so wait and see i guess. Once you cvs remove something
    you cant bring it back with the same name without mucking with cvs internals.
    Also if we really need to go this route probably want event listener for views 
    in general - strand, annot, result, sites, revcomp... There already is one
    for revcomp used by synteny.
*/
public interface StrandVisibilityListener {
  public void strandIsVisible(boolean isForward);
}
