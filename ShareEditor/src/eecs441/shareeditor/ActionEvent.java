package eecs441.shareeditor;

public class ActionEvent
{
  int CursorStart;
  String before;
  String after;
  
  public ActionEvent() {
    CursorStart = -1;
    before = "";
    after = ""; 
  }
  public void setCursorStart(int pos) {
    CursorStart = pos;
  }
  public int getCursorStart() {
    return CursorStart;
  }
  public int insLength() {
    return after.length();
  }
  public int delLength() {
    return before.length();
  }
  public String insString() {
    return after;
  }
  public String delString() {
    return before;
  }
  public void setInsertString(String in) {
    after = in;
  }
  public void setDeleteString(String del) {
    before = del;
  }
  public void clear() {
    CursorStart = -1;
  }
  public ActionEvent applyActionEvent(ActionEvent e) {
    ActionEvent dummy = new ActionEvent();
    if(e.getCursorStart() >= CursorStart + before.length());
    else if(e.getCursorStart() + e.delLength() <= CursorStart) CursorStart += e.insLength() - e.delLength();
    else if(CursorStart >= e.getCursorStart()) {
      if(before.length() > 0) before = before.substring(e.getCursorStart() + e.delLength() - CursorStart, before.length());
      CursorStart = e.getCursorStart();
    }
    else if(CursorStart < e.getCursorStart()) {
      if((e.getCursorStart() + e.delLength() < CursorStart + before.length())&&(e.getCursorStart() + e.insLength() < CursorStart + before.length())) {
        dummy.setCursorStart(e.getCursorStart() + e.insLength());
        dummy.setDeleteString(before.substring(e.getCursorStart() + e.delLength() - CursorStart, before.length()));
      }
      before = before.substring(0, e.getCursorStart() - CursorStart);
    }
    
    return dummy;
  }
  public ActionEvent invert() {
    ActionEvent reverse = new ActionEvent();
    reverse.setCursorStart(CursorStart);
    reverse.setInsertString(before);
    reverse.setDeleteString(after);
    return reverse;
  }
  public boolean isEmpty() {
    return CursorStart < 0;
  }
}
