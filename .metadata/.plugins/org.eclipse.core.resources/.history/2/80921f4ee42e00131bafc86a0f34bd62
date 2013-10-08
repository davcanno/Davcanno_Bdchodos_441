package eecs441.shareeditor;

import java.util.Stack;
import java.util.Vector;

public class ActionStack
{
   Stack<ActionEvent> ActionStack;
   int size;
   
   public ActionStack() {
     ActionStack = new Stack<ActionEvent>();
     size = 0;
   }
   public void clear() {
     ActionStack.removeAllElements();
     size = 0;
   }
   public void push(ActionEvent e) {
     ActionStack.push(e);
     size++;
   }
   public ActionEvent pop() {
     if(size <= 0) return new ActionEvent();
     size--;
     return ActionStack.pop();     
   }
   public void ApplyActionEvent(ActionEvent e) {
     Stack<ActionEvent> tempStack = new Stack<ActionEvent>();
     for(int i = 0; (i < size)&&(i < 10); i++) {
       ActionEvent t = ActionStack.pop();
       ActionEvent t2 = t.applyActionEvent(e);
       if(!t2.isEmpty()) {
         tempStack.push(t2);
         i++;
       }
       tempStack.push(t);
     }
     clear();
     while(!tempStack.empty()) push(tempStack.pop());
   }
   public boolean isEmpty() {
     return ActionStack.empty();
   }
}
