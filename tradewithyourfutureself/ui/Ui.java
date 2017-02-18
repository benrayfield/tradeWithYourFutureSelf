/** Ben F Rayfield offers this software opensource GNU GPL 2+ */
package tradewithyourfutureself.ui;
import humanaicore.common.Time;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.Window.Type;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

/** Point on side of screen moves up and down at speed you change with 1 click.
Speed is seen as another point where it will be in 10 minutes.
Set the velocity upward to as fast as you're getting work done. When you take a break,
set the velocity downward. Save break times for later or take them now.
It will track your progress.
The position of the dot feels like money you trade with your future self.
*/
public class Ui extends JPanel implements MouseListener, MouseMotionListener{
	
	public static final String help = " TRADE WITH YOUR FUTURE SELF - LEFT CLICK POSITION - RIGHT CLICK VELOCITY - WORK UP - PLAY DOWN";
	protected String displayText = help;
	
	protected boolean mouseIsIn, mouseWasEverIn;
	
	/** position in range 0 to 1. This feels like money you trade with your future self.
	Up is getting work done. Down is screwing around.
	*/
	protected double position=.5;
	
	/** (negative of, since graphics defines down as positive) How fast you're working minus breaks */ 
	protected double velocity=.0001;
	
	protected double displayStdDevOfPositionBlob=.02;
	
	protected double displayStdDevOfVelocityBlob=.02;
	
	protected double displayStdDevOfMiddleBlob=.01;
	
	/** How long it takes the position blob to reach the other blob that represents its speed vector */
	protected double intervalSeconds=60*10;
	
	protected int heightCache = 105;
	
	protected double lastTimePainted = Time.time();
	
	protected Set<Integer> mouseButtonsDown = new HashSet<Integer>();
	
	protected double repaintWhenMoveThisManyPixels = .2;
	
	protected int mouseY, mouseWentDownAtY=Integer.MAX_VALUE;
	
	public Ui(){
		addMouseListener(this);
		addMouseMotionListener(this);
	}
	
	public void paint(Graphics g){
		heightCache = getHeight();
		double now = Time.time();
		double sinceLastPaint = now-lastTimePainted;
		lastTimePainted = now;
		position += velocity*sinceLastPaint;
		double positionAfterInterval = position+velocity*intervalSeconds;
		//target never goes off screen, decays toward it when too close
		if(positionAfterInterval < 0) setVelocityByClickAtFraction(0);
		else if(positionAfterInterval > 1) setVelocityByClickAtFraction(1);
		int w = getWidth();
		for(int h=0; h<heightCache; h++){
			double hFraction = (double)h/heightCache;
			float red = (float)bellWhoseTopIs1((.5-hFraction)/(displayStdDevOfPositionBlob/2))*.4f;
			float green = (float)bellWhoseTopIs1((position-hFraction)/displayStdDevOfPositionBlob)/2;
			float blue = (float)bellWhoseTopIs1((positionAfterInterval-hFraction)/displayStdDevOfVelocityBlob);
			green += blue/3;
			green -= red;
			red = Math.max(0, Math.min(red, 1));
			green = Math.max(0, Math.min(green, 1));
			blue = Math.max(0, Math.min(blue, 1));
			g.setColor(new Color(red,green,blue));
			g.drawLine(0, h, w, h);
		}
		g.setColor(Color.white);
		paintString(g,displayText);
	}
	
	//protected int fontSize(char c)
	
	public void paintString(Graphics g, String s){
		//Font f = g.getFont();
		//g.setFont(new Font(.getName(), g.getFont().getStyle(), 9));
		g.setFont(new Font("Monospaced", 0, 12));
		for(int i=0; i<s.length(); i++){
			g.drawString(s.substring(i,i+1), 0, 10*(i+1));
		}
	}
	
	/** Can update earlier for mouse events */
	public double maxUpdateInterval(){
		return repaintWhenMoveThisManyPixels/pixelsPerSecond();
	}
	
	public double pixelsPerSecond(){
		return velocity*getHeight();
	}
	
	public static void main(String[] args){
		String s = "Trade With Your Future Self (thin bar on left of screen)";
		JFrame window = new JFrame(s);
		Ui ui = new Ui();
		ui.setToolTipText(s);
		window.add(new Ui());
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		window.setUndecorated(true);
		int avoidTop=20;
		int avoidBottom=30;
		window.setSize(7, screen.height-avoidTop-avoidBottom);
		window.setLocation(0, avoidTop);
		window.setAlwaysOnTop(true);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		/*if(SystemTray.isSupported()){
			TrayIcon t = new TrayIcon(ui.icon(), "Trade With Your Future Self");
			try{
				SystemTray.getSystemTray().add(t);
			}catch(AWTException e){ throw new RuntimeException(e); }
			window.setType(Type.UTILITY);
		}*/
		window.setType(Type.UTILITY);
		window.setVisible(true);
		//int height = window.getHeight();
		new Thread(){
			public void run(){
				while(true){
					//double pixelsPerSecond = r.speed*r.heightCache; //FIXME why isnt r.heightCache updating?
					
					//TODO paint every repaintAfterMoveHowManyPixels pixels moved,
					//and figure out why r.speed and r.heightCache differ from when they're written by other thread.
					/*double pixelsPerSecond;
					synchronized(r){
						pixelsPerSecond = r.speed*height;
						System.out.println("r.speed="+r.speed);
					}
					double repaintAfterMoveHowManyPixels = .2;
					double sleep = repaintAfterMoveHowManyPixels/pixelsPerSecond;
					Time.sleepNoThrow(sleep);
					*/
					
					Time.sleepNoThrow(.5);
					//System.out.println("r.heightCache="+r.heightCache+" Sleep time "+repaintAfterMoveHowManyPixels/pixelsPerSecond);
					//System.out.println("Sleep time "+repaintAfterMoveHowManyPixels/pixelsPerSecond);
					//window.repaint();
					ui.event();
				}
			}
		}.start();
	}
	
	protected Image icon(){
		try{
			String path = "/data/tradewithyourfutureself/icon.jpg";
			File f = new File(path);
			return ImageIO.read(f.exists() ? new FileInputStream(f) : Ui.class.getResourceAsStream(path));
		}catch(IOException e){ throw new RuntimeException(e); }
	}
	
	/** unit bellcurve height * sqrt(2*pi) */
	public static double bellWhoseTopIs1(double stdDev){
		return Math.exp(-stdDev*stdDev);
	}
	
	public void setVelocityByClickAtFraction(double heightFraction){
		velocity = (heightFraction-position)/intervalSeconds;
		//displayText = "VELOCITY - WHERE IT WILL BE IN 10 MINUTES - WORK UP - PLAY DOWN";
		displayText = " MOVE PER 10 MINUTES";
	}
	
	public void setPositionByClickAtFraction(double heightFraction){
		position = heightFraction;
		//displayText = "POSITION - FEELS LIKE MONEY TRADING WITH FUTURE SELF - WORK UP - PLAY DOWN";
		displayText = " POSITION";
	}
	
	protected void event(){
		double heightFraction = (double)mouseY/getHeight();
		if(mouseWentDownAtY <= 10){
			System.exit(0);
		}else if(mouseButtonsDown.contains(MouseEvent.BUTTON1)){
			setVelocityByClickAtFraction(heightFraction);
		}else if(mouseButtonsDown.contains(MouseEvent.BUTTON3)){
			setPositionByClickAtFraction(heightFraction);
		}else if(mouseIsIn){
			displayText = "X";
		}else{
			displayText = "";
		}
		repaint();
	}

	public void mouseClicked(MouseEvent e){}

	public void mousePressed(MouseEvent e){
		mouseButtonsDown.add(e.getButton());
		mouseWentDownAtY = mouseY; 
		event();
	}

	public void mouseReleased(MouseEvent e){
		mouseButtonsDown.remove(e.getButton());
		displayText = "";
		event();
	}

	public void mouseEntered(MouseEvent e){
		mouseWasEverIn = mouseIsIn = true;
		event();
	}

	public void mouseExited(MouseEvent e){
		mouseIsIn = false;
		event();
	}

	public void mouseDragged(MouseEvent e){
		mouseMoved(e);
	}

	public void mouseMoved(MouseEvent e){
		mouseWasEverIn = mouseIsIn = true;
		mouseY = e.getY();
		event();
	}

}
