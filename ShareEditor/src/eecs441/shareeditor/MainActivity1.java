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
import android.content.Context;
import android.content.DialogInterface;
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

public class MainActivity1 extends Activity
{
  
  MainView mainWindow;

  LinearLayout connectScreen;
  boolean keyboardUp;
  private static String TAG = "ShareEditor";
  
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
  private String fixThis="";
  private String removeText="";
  private int userId;
  private boolean received = false;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    userId = 2;
    keyboardUp = false;
    final InputMethodManager inputManager = (InputMethodManager)
        getSystemService(Context.INPUT_METHOD_SERVICE);
    getWindow().setSoftInputMode(
        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    
    mainWindow = new MainView(this);
    
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
        if(!received && count > 0) {
          if(fixThis.length() != 0) {
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
          removeText += s.toString().substring(start, start+count);
        }
        if(count > 0) {
          received = false;
        }
      }

      @Override
      public void afterTextChanged(Editable arg0)
      {
        
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count)
      {
        if(!received && count > 0) {
          if(removeText.length() != 0) {
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
          fixThis += s.toString().substring(start, start+count);
        }
        received = false;
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
        Event.Builder event = Event.newBuilder();
        event.setId(userId);
        event.setCursorpos(mainWindow.getCursorPosition());
        if(fixThis.length() != 0) {
          event.setBeforetext("");
          event.setAftertext(fixThis);
          fixThis = "";
        }
        else {
          event.setBeforetext(removeText);
          event.setAftertext("");  
          removeText="";
        }
        
        try
        {
          myClient.broadcast(event.build().toByteArray(), "lol");
        }
        catch( CollabrifyException e1 )
        {
          e1.printStackTrace();
        }
        if(mainWindow.getCursorPosition() < mainWindow.getLength()) {
          mainWindow.setCursorPosition(mainWindow.getCursorPosition()+1);
        }
      }
    });
    mainWindow.setCursorLeftListener(new OnClickListener() {
      public void onClick(View clickedButton) {
        Event.Builder event = Event.newBuilder();
        event.setId(userId);
        event.setCursorpos(mainWindow.getCursorPosition());
        if(fixThis.length() != 0) {
          event.setBeforetext("");
          event.setAftertext(fixThis);
          fixThis = "";
        }
        else {
          event.setBeforetext(removeText);
          event.setAftertext("");  
          removeText="";
        }
        
        try
        {
          myClient.broadcast(event.build().toByteArray(), "lol");
        }
        catch( CollabrifyException e1 )
        {
          e1.printStackTrace();
        }
        if(mainWindow.getCursorPosition()> 0) {
          mainWindow.setCursorPosition(mainWindow.getCursorPosition()-1);
        }
      }
    });
    
    setTitle("Text Editor");
    
    /////////////////////////////////////////////////////////////////////////
    /////       Creation of secondary screen for connections            /////
    /////////////////////////////////////////////////////////////////////////
    
    connectScreen = new LinearLayout(this);
    connectScreen.setOrientation(LinearLayout.VERTICAL);
    connectScreen.setWeightSum(100f);
    setTitle("Collabrify Sessions");
    
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
        Random rand = new Random();
        sessionName = "Test " + rand.nextInt(Integer.MAX_VALUE);
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
        connectButton.setText(sessionName);
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
          if( myClient.inSession() )
            myClient.currentSessionEnded();
            connectButton.setText("Connect");
        }
        catch( CollabrifyException e )
        {
          Log.e(TAG, "error4", e);
        }
      }
    });

    connectScreen.addView(connectButton);
    connectScreen.addView(getSessionButton);
    connectScreen.addView(leaveSessionButton);
    connectScreen.addView(endSessionButton);
    
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
/////////////////////////////////////////////////////////////////////////
/////       Protocol buffer workspace for now                       /////
/////////////////////////////////////////////////////////////////////////           
            Event eventRecvd = null;
            try
            {
              eventRecvd = Event.parseFrom(data);
            }
            catch( InvalidProtocolBufferException e )
            {
              e.printStackTrace();
            }
            //DO ACTION HERE
            if(eventRecvd.getId() != userId) {
              String after = eventRecvd.getAftertext();
              String before = eventRecvd.getBeforetext();
              int cursor = eventRecvd.getCursorpos();
              if(after.length() > 0) {
                //INSERT
                received = true;
                mainWindow.getText().replace(cursor-after.length(), cursor-after.length(), after);
              }
              else {
                //REMOVE
                received = true;
                mainWindow.getText().replace(cursor, cursor+before.length(), after);
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
        final AlertDialog.Builder builder = new AlertDialog.Builder(
            MainActivity1.this);
          
          builder.setTitle("Choose Session").setItems(
            sessionNames.toArray(new String[sessionList.size()]),
            new DialogInterface.OnClickListener()
            {
              @Override
              public void onClick(DialogInterface dialog, int which)
              {
                try
                {
                  sessionId = sessionList.get(which).id();
                  sessionName = sessionList.get(which).name();
                  myClient.joinSession(sessionId, null);
                  connectButton.setText(sessionName);
                }
                catch( CollabrifyException e )
                {
                  Log.e(TAG, "error5", e);
                }
              }
            });

        runOnUiThread(new Runnable()
        {

          @Override
          public void run()
          {
            builder.show();
          }
        });
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
            connectButton.setText(sessionName);
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
            connectButton.setText(sessionName);
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
    textEditor.setText("Edit Text");
    textEditor.setOnClickListener(new OnClickListener()
    {

      @Override
      public void onClick(View v)
      {
        setContentView(mainWindow);
        mainWindow.setCursorVisible();
      }
    });
    connectScreen.addView(textEditor);
    
    setContentView(mainWindow);
  }
  public static final int MENU_ADD = Menu.FIRST;

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
      super.onCreateOptionsMenu(menu);

      menu.add(Menu.NONE, MENU_ADD, Menu.NONE, "Manage Sessions");
      return true;
  }

     @Override
      public boolean onOptionsItemSelected(MenuItem item)
      {
          switch(item.getItemId())
          {
              case MENU_ADD:
                setContentView(connectScreen);
                if(keyboardUp) {
                  mainWindow.setInsertText("edit");
                  final InputMethodManager inputManager = (InputMethodManager)
                      getSystemService(Context.INPUT_METHOD_SERVICE);
                  inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                      InputMethodManager.HIDE_NOT_ALWAYS);
                  keyboardUp = false;
                }
              return true;

          default:
              return super.onOptionsItemSelected(item);
          }
      }
}

