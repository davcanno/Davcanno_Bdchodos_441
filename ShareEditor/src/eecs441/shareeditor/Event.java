package eecs441.shareeditor;

public class Event
{
  int Id;
  int type;
  int CursorStart;
  String change;
  public Event(int id) {
    Id = id;
    type = 0;
    CursorStart = 0;
    change = "";
  }
  public String getString() {
    return change;
  }
  public int getId() {
    return Id;
  }
  public void appendString(String input) {
     change = change + input;
  }
  public int getLength() {
    return change.length();
  }
  public void setCursorStart(int position) {
    CursorStart = position;
  }
  public int getCursorStart() {
    return CursorStart;
  }
  public void setId(int id) {
    Id = id;
  }
  public void clearString() {
    change = "";
  }
}
