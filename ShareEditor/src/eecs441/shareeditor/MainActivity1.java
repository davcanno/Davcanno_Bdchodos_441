package eecs441.shareeditor;

import com.google.protobuf.*;
import eecs441.shareeditor.proto.TestProto.Event;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import eecs441.shareeditor.R;
import edu.umich.imlc.android.common.Utils;
import edu.umich.imlc.collabrify.client.CollabrifyAdapter;
import edu.umich.imlc.collabrify.client.CollabrifyListener;
import edu.umich.imlc.collabrify.client.CollabrifyClient;
import edu.umich.imlc.collabrify.client.CollabrifySession;
import edu.umich.imlc.collabrify.client.exceptions.CollabrifyException;
import edu.umich.imlc.collabrify.client.exceptions.ConnectException;
import edu.umich.imlc.collabrify.client.exceptions.LeaveException;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity1 extends Activity
{
  
  MainView mainWindow;

  LinearLayout connectScreen;
  boolean keyboardUp;
  private static String TAG = "ShareEditor";
  
  ActionEvent bufferAction;
  ActionStack UndoStack;
  ActionStack RedoStack;
  int MyCounter;
  
  private EditText sessionText;
  
  Button textEditor;
  
  private CollabrifyClient myClient;
  private Button connectButton;
  private Button getSessionButton;
  private Button leaveSessionButton;
  private Button endSessionButton;
  private CollabrifyListener collabrifyListener;
  private ArrayList<String> tags = new ArrayList<String>();
  private long sessionId;
  private String sessionName;
  private int userId;
  private boolean received = false;
  
  @SuppressLint("NewApi")
@Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Enter User ID Number");

    // Set up the input
    final EditText input = new EditText(this);
    // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
    input.setInputType(InputType.TYPE_CLASS_NUMBER);
    builder.setView(input);

    // Set up the buttons
    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
        @Override
        public void onClick(DialogInterface dialog, int which) {
            userId = Integer.parseInt(input.getText().toString());
            setContentView(connectScreen);
        }
    });

    builder.show();
    keyboardUp = false;
    final InputMethodManager inputManager = (InputMethodManager)
        getSystemService(Context.INPUT_METHOD_SERVICE);
    getWindow().setSoftInputMode(
        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    
    mainWindow = new MainView(this);
    
    bufferAction = new ActionEvent();
    RedoStack = new ActionStack();
    UndoStack = new ActionStack();
    MyCounter = 0;
    
   mainWindow.setCursorTouchListener(new OnTouchListener() {
     private int x;
     private int y;
   @Override
   public boolean onTouch(View v, MotionEvent event) {
     Layout layout = ((EditText) v).getLayout();
     switch( event.getAction() ) {
       case MotionEvent.ACTION_DOWN:
       x = (int)event.getX();
       y = (int)event.getY();
       return true;
       case MotionEvent.ACTION_UP:
     if (layout!=null){
         int line = layout.getLineForVertical(y);
         int line2 = layout.getLineForVertical((int)event.getY());
         if((line2 > line+1)||(line2 < line-1)) return true;
         int offset = layout.getOffsetForHorizontal(line, x);
         int offset2 = layout.getOffsetForHorizontal(line, (int)event.getX());
         if((offset2 > offset+2)||(offset2 < offset-2)) return true;
         mainWindow.setCursorPosition(offset);
       }
     return true;
     }
     return true;
   }
   });
  
    mainWindow.setTextChangedListener(new TextWatcher() {
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if(!received) {

          bufferAction = new ActionEvent();

          bufferAction.setDeleteString(s.toString().substring(start, start+count));
          MyCounter++;
          /*if(fixThis.length() != 0) {
            Event.Builder event = Event.newBuilder();
            event.setId(userId);
            event.setCursorpos(mainWindow.getCursorPosition());
            event.setBeforetext("");
            event.setAftertext(fixThis);
            fixThis = "";          
            try
            {
              myClient.broadcast(event.build().toByteArray(), "lol");
            }
            catch( CollabrifyException e1 )
            {
              e1.printStackTrace();
            }
          }
          removeText += s.toString().substring(start, start+count);*/
        }

      }

      @Override
      public void afterTextChanged(Editable arg0)
      {
        received = false;
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count)
      {
        if(!received) {
          bufferAction.setInsertString(s.toString().substring(start,start+count));
          bufferAction.setCursorStart(start);
          Event.Builder event = Event.newBuilder();
          event.setId(userId);
          event.setCursorpos(bufferAction.getCursorStart());
          event.setBeforetext(bufferAction.delString());
          event.setAftertext(bufferAction.insString());           
          try
          {
            myClient.broadcast(event.build().toByteArray(), "lol");
            //MyCounter++;
          }
          catch( CollabrifyException e1 )
          {
            e1.printStackTrace();
          }
          RedoStack.clear();
          UndoStack.push(bufferAction.invert());
          /*if(removeText.length() != 0) {
            Event.Builder event = Event.newBuilder();
            event.setId(userId);
            event.setCursorpos(mainWindow.getCursorPosition()-count);
            event.setBeforetext(removeText);
            event.setAftertext("");  
            removeText="";         
            try
            {
              myClient.broadcast(event.build().toByteArray(), "lol");
            }
            catch( CollabrifyException e1 )
            {
              e1.printStackTrace();
            }
          }
          System.out.println("HERE"+s.toString().substring(start, start+count));
          fixThis += s.toString().substring(start, start+count);*/
        }
        //received = false;
      }
    });
    
    mainWindow.setInsertClickListener(new OnClickListener() {
      @Override
      public void onClick(View ClickedButton) {
        if(keyboardUp) {
          mainWindow.setInsertText("edit");
          inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
              InputMethodManager.HIDE_NOT_ALWAYS);
          keyboardUp = false;
        } else {
        inputManager.showSoftInput(getCurrentFocus(),  0);
        mainWindow.setInsertText("hide");
        keyboardUp = true;
        }
      }
    });   
    
    mainWindow.setCursorRightListener(new OnClickListener() {
      public void onClick(View clickedButton) {
        if(mainWindow.getCursorPosition() < mainWindow.getLength()) {
          mainWindow.setCursorPosition(mainWindow.getCursorPosition()+1);
        }
      }
    });
    mainWindow.setCursorLeftListener(new OnClickListener() {
      public void onClick(View clickedButton) {
        if(mainWindow.getCursorPosition()> 0) {
          mainWindow.setCursorPosition(mainWindow.getCursorPosition()-1);
        }
      }
    });
    mainWindow.setUndoClickListener(new OnClickListener() {
      public void onClick(View clickedButton) {
        if(!UndoStack.isEmpty()) {
          ActionEvent temp = UndoStack.pop();
          received = true;
          mainWindow.applyActionEvent(temp);
          RedoStack.push(temp.invert());
          Event.Builder event = Event.newBuilder();
          event.setId(userId);
          event.setCursorpos(temp.getCursorStart());
          event.setBeforetext(temp.delString());
          event.setAftertext(temp.insString());           
          try
          {
            myClient.broadcast(event.build().toByteArray(), "lol");
            MyCounter++;
          }
          catch( CollabrifyException e1 )
          {
            e1.printStackTrace();
          }
        }
      }
    });
    mainWindow.setRedoClickListener(new OnClickListener() {
      public void onClick(View clickedButton) {
        if(!RedoStack.isEmpty()) {
          ActionEvent temp = RedoStack.pop();
          received = true;
          mainWindow.applyActionEvent(temp);
          UndoStack.push(temp.invert());
          Event.Builder event = Event.newBuilder();
          event.setId(userId);
          event.setCursorpos(temp.getCursorStart());
          event.setBeforetext(temp.delString());
          event.setAftertext(temp.insString());           
          try
          {
            myClient.broadcast(event.build().toByteArray(), "lol");
            MyCounter++;
          }
          catch( CollabrifyException e1 )
          {
            e1.printStackTrace();
          }
        }
      }
    });
    
    /////////////////////////////////////////////////////////////////////////
    /////       Creation of secondary screen for connections            /////
    /////////////////////////////////////////////////////////////////////////
    
    connectScreen = new LinearLayout(this);
    connectScreen.setOrientation(LinearLayout.VERTICAL);
    connectScreen.setWeightSum(100f);
    connectScreen.setBackgroundColor(0xff55ddff);
    setTitle("WeWrite");
    
    sessionText = new EditText(this);
    sessionText.setText("");
    sessionText.setRawInputType(InputType.TYPE_TEXT_VARIATION_NORMAL | InputType.TYPE_TEXT_VARIATION_FILTER | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    sessionText.setGravity(0x01);
    sessionText.setTextSize(24);
    sessionText.setBackgroundColor(0xffffffff);
    
    connectButton = new Button(this);
    connectButton.setText("Connect");
    getSessionButton = new Button(this);
    getSessionButton.setText("Join");
    leaveSessionButton = new Button(this);
    leaveSessionButton.setText("Leave");
    endSessionButton = new Button(this);
    endSessionButton.setText("End");
    // enable logging
    
    //Logger.getLogger("edu.umich.imlc.collabrify.client")
    //    .setLevel(LOGGING_LEVEL);

    connectButton.setOnClickListener(new OnClickListener()
    {

      @Override
      public void onClick(View v)
      {        
        sessionName = sessionText.getText().toString();
        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
            InputMethodManager.HIDE_NOT_ALWAYS);
        if(sessionName.length() > 0) {
          try
          {
            myClient.createSession(sessionName, tags, null, 0);
          }
          catch( ConnectException e )
          {
            e.printStackTrace();
          }
          catch( CollabrifyException e )
          {
            e.printStackTrace();
          }
          Log.i(TAG, "Session name is " + sessionName);
          connectButton.setText("Connected to " + sessionName);
          //sessionText.setTextIsSelectable(false);
          setTitle("WeWrite - " + sessionName);
        }
        else Toast.makeText(getBaseContext(), "Invalid Session Name", Toast.LENGTH_SHORT).show();
      }
    });

    getSessionButton.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        try
        {
          myClient.requestSessionList(tags);
          inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
              InputMethodManager.HIDE_NOT_ALWAYS);
        }
        catch( Exception e )
        {
          Log.e(TAG, "error3", e);
        }
      }
    });

    leaveSessionButton.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        try
        {
          if( myClient.inSession() )
            myClient.leaveSession(false);
            connectButton.setText("Connect");
            sessionText.setText("");
            //sessionText.setTextIsSelectable(true);
            setTitle("WeWrite - No Session");
        }
        catch( CollabrifyException e )
        {
          Log.e(TAG, "error4", e);
        }
      }
    });
    
    endSessionButton.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        try
        {
          if( myClient.inSession() && myClient.currentSessionOwner().getId() == 
              myClient.currentSessionParticipantId()) {
            Event.Builder event = Event.newBuilder();
            event.setId(userId);
            event.setEnded(true);
            myClient.broadcast(event.build().toByteArray(), "lol");
            myClient.leaveSession(true);
            connectButton.setText("Connect");
            sessionText.setText("");
            //sessionText.setTextIsSelectable(true);
            setTitle("WeWrite - No Session");
          }
        }
        catch( CollabrifyException e )
        {
          Log.e(TAG, "error4", e);
        }
      }
    });
    final GradientDrawable gd = new GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        new int[] {0xFFda3314,0xFFba1314});  
    gd.setCornerRadius(0f);
    
    connectButton.setTypeface(Typeface.DEFAULT_BOLD, Typeface.BOLD);
    connectButton.setTextColor(0xffffffff);
    connectButton.setBackground(gd);
    getSessionButton.setTypeface(Typeface.DEFAULT_BOLD, Typeface.BOLD);
    getSessionButton.setTextColor(0xffffffff);
    getSessionButton.setBackground(gd);
    leaveSessionButton.setTypeface(Typeface.DEFAULT_BOLD, Typeface.BOLD);
    leaveSessionButton.setTextColor(0xffffffff);
    leaveSessionButton.setBackground(gd);
    endSessionButton.setTypeface(Typeface.DEFAULT_BOLD, Typeface.BOLD);
    endSessionButton.setTextColor(0xffffffff);
    endSessionButton.setBackground(gd);
    
    
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    params.setMargins(150, 20, 150, 20);
    LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    params2.setMargins(100, 300, 100, 20);
    connectScreen.addView(sessionText, params2);
    connectScreen.addView(connectButton, params);
    connectScreen.addView(getSessionButton, params);
    connectScreen.addView(leaveSessionButton, params);
    connectScreen.addView(endSessionButton, params);
    
    collabrifyListener = new CollabrifyAdapter()
    {
      @Override
      public void onDisconnect()
      {
        Log.i(TAG, "disconnected");
        runOnUiThread(new Runnable()
        {

          @Override
          public void run()
          {
            connectButton.setText("Connect");
          }
        });
      }

      @Override
      public void onReceiveEvent(final long orderId, int subId,
          String eventType, final byte[] data)
      {
        Utils.printMethodName(TAG);
        Log.d(TAG, "RECEIVED SUB ID:" + subId);
        runOnUiThread(new Runnable()
        {
          @Override
          public void run()
          {
            Utils.printMethodName(TAG);
            //String message = new String(data);
            System.out.println("RECVD MESSG?");           
            Event eventRecvd = null;
            try
            {
              eventRecvd = Event.parseFrom(data);
            }
            catch( InvalidProtocolBufferException e )
            {
              e.printStackTrace();
            }
            System.out.println("#########" + userId + " " + eventRecvd.getId());
            //DO ACTION HERE
            if(eventRecvd.getId() != userId) {
              if(eventRecvd.hasEnded()) {
                connectButton.setText("Connect");
                //sessionText.setTextIsSelectable(true);
                sessionText.setText("");
                setTitle("WeWrite - No Session");
                try
                {
                  myClient.leaveSession(false);
                }
                catch( LeaveException e )
                {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
                }
                catch( CollabrifyException e )
                {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
                }
                setContentView(connectScreen); 
              }
              else {
                ActionEvent A = new ActionEvent();
                if(eventRecvd.hasAftertext())
                  A.setInsertString(eventRecvd.getAftertext());
                if(eventRecvd.hasBeforetext())
                  A.setDeleteString(eventRecvd.getBeforetext());
                if(eventRecvd.hasCursorpos())
                  A.setCursorStart(eventRecvd.getCursorpos());
                System.out.println("Cursor: " + eventRecvd.getCursorpos());
              
     ///////////////LOOK HERE SYNTAX IS NOT RIGHT BUT I DONT KNOW IT////////////////////////////
                int RecCounter = (int) orderId;
                //received = true;
                for(int i = 0; i < MyCounter - RecCounter; i++) {
                  received = true;
                  ActionEvent temp = UndoStack.pop();
                  mainWindow.applyActionEvent(temp);
                  RedoStack.push(temp.invert());
                }
                received = true;
                mainWindow.applyActionEvent(A);
                UndoStack.ApplyActionEvent(A);
                RedoStack.ApplyActionEvent(A);
               for(int i = 0; i < MyCounter - RecCounter; i++) {
                  received = true;
                  ActionEvent temp = RedoStack.pop();
                  mainWindow.applyActionEvent(temp);
                  UndoStack.push(temp.invert());
                }
                MyCounter++;
              }
            }
            //RIGHT FREAKIN HERE
          }
        });
      }

      @Override
      public void onReceiveSessionList(final List<CollabrifySession> sessionList)
      {
        if( sessionList.isEmpty() )
        {
          Log.i(TAG, "No session available");
          return;
        }
        final List<String> sessionNames = new ArrayList<String>();
        for( CollabrifySession s : sessionList )
        {
          sessionNames.add(s.name());
        }
        sessionName = sessionText.getText().toString();
        int index = -1;
        for(int i = 0; i < sessionNames.size(); i++) {
          if(sessionNames.get(i).equals(sessionName)) {
            index = i;
            i = sessionNames.size();
          }
        }
        if(index >= 0) {
          try
          {
            sessionId = sessionList.get(index).id();
            myClient.joinSession(sessionId, null);
            connectButton.setText("Connected to " + sessionName);
            setTitle("WeWrite - " + sessionName);
            //sessionText.setTextIsSelectable(false);

            setContentView(mainWindow);
            mainWindow.setCursorVisible();
          }
          catch( CollabrifyException e )
          {
            Log.e(TAG, "error5", e);
          }
        }
        else {
          runOnUiThread(new Runnable()
          {
            @Override
            public void run()
            {
              Toast.makeText(getBaseContext(), "Invalid Session Name", Toast.LENGTH_SHORT).show();
            }
          });
        }
      }

      @Override
      public void onSessionCreated(long id)
      {
        Log.i(TAG, "Session created, id: " + id);
        sessionId = id;
        runOnUiThread(new Runnable()
        {

          @Override
          public void run()
          {
            //connectButton.setText(sessionName);
          }
        });
      }

      @Override
      public void onError(CollabrifyException e)
      {
        Log.e(TAG, "error1", e);
      }

      @Override
      public void onSessionJoined(long maxOrderId, long baseFileSize)
      {
        Log.i(TAG, "Session Joined");
        runOnUiThread(new Runnable()
        {

          @Override
          public void run()
          {
            //connectButton.setText(sessionName);
          }
        });
      }
    };


    boolean getLatestEvent = false;
    
    // Instantiate client object
    try
    {
      myClient = new CollabrifyClient(this, "gocannon16@gmail.com", "user display name",
          "441fall2013@umich.edu", "XY3721425NoScOpE", getLatestEvent,
          collabrifyListener);
    }
    catch( CollabrifyException e )
    {
      e.printStackTrace();
    }
    tags.add("sample");
    
    /////////////////////////////////////////////////////////////////////////
    /////       Protocol buffer workspace for now                       /////
    /////////////////////////////////////////////////////////////////////////
    
    textEditor = new Button(this);
    textEditor.setText("Edit Document");
    textEditor.setOnClickListener(new OnClickListener()
    {

      @Override
      public void onClick(View v)
      {
        setContentView(mainWindow);
        //setTitle("WeWrite - " + sessionName);
        mainWindow.setCursorVisible();
      }
    });
    textEditor.setTypeface(Typeface.DEFAULT_BOLD, Typeface.BOLD);
    textEditor.setTextColor(0xffffffff);
    textEditor.setBackground(gd);
    connectScreen.addView(textEditor, params);
    
    setContentView(mainWindow);
    //setContentView(connectScreen);
  }
  public static final int MENU_ADD = Menu.FIRST;
  public static final int LEAVE = Menu.FIRST+1;
  public static final int END = Menu.FIRST+2;

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
      super.onCreateOptionsMenu(menu);

      menu.add(Menu.NONE, MENU_ADD, Menu.NONE, "Manage Sessions");
      //menu.add(Menu.NONE, LEAVE, Menu.NONE, "Leave Session");
      //menu.add(Menu.NONE, END, Menu.NONE, "End Session");
      return true;
  }

     @Override
      public boolean onOptionsItemSelected(MenuItem item)
      {
          switch(item.getItemId())
          {
              case MENU_ADD:
                setContentView(connectScreen);
                /*
                if(keyboardUp) {
                  mainWindow.setInsertText("edit");
                  final InputMethodManager inputManager = (InputMethodManager)
                      getSystemService(Context.INPUT_METHOD_SERVICE);
                  inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                      InputMethodManager.HIDE_NOT_ALWAYS);
                  keyboardUp = false;
                  
                }*/
                return true;
                /*
              case LEAVE:
                try
                {
                  if(myClient.inSession() )
                    myClient.leaveSession(false);
                    connectButton.setText("Connect");
                    setTitle("WeWrite");
                }
                catch(CollabrifyException e)
                {
                  Log.e(TAG, "error4", e);
                }
                setContentView(connectScreen);
                return true;
                
              case END:
                try
                {
                  if(myClient.inSession() && myClient.currentSessionOwner().getId() ==
                      myClient.currentSessionParticipantId()) {
                    Event.Builder event = Event.newBuilder();
                    event.setId(userId);
                    event.setEnded(true);
                    myClient.broadcast(event.build().toByteArray(), "lol");
                    myClient.leaveSession(true);
                    connectButton.setText("Connect");
                    setTitle("WeWrite");
                  }
                }
                catch(CollabrifyException e)
                {
                  Log.e(TAG, "error4", e);
                }
                setContentView(connectScreen);
                return true;
                */

              default:
                 return super.onOptionsItemSelected(item);
          }
      }
}

