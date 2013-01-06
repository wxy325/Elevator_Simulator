//Elevator.java
//实现电梯楼层数字按钮及电梯移动

import java.awt.*;
import java.awt.event.*;
import java.sql.Time;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.*;
import javax.swing.border.*;

public class Elevator extends JPanel
{
	//界面相关data member
	private JPanel upPanel = new JPanel();				//包括电梯及楼层按钮
	private JPanel elevatorButtonPanel = new JPanel();
	private ElevatorPanel elevatorPanel;		//电梯楼
	private JPanel openCloseButtonPanel = new JPanel();	//包括电梯开门关门按钮
	private JButton openButton = new JButton("Open");
	private JButton closeButton = new JButton("Close");
	private ElevatorNumberButton[] elevatorButtons = new ElevatorNumberButton[MainWindows.FLOOR_NUMBER];
	
	//状态相关data member
	private UpDownButtonPanel m_UpDownButtons;
	
	public Elevator(int n,UpDownButtonPanel upDownButtons)
	{
		super();
		//状态变量初始化
		m_UpDownButtons = upDownButtons;
		
		
		//界面初始化
		setBorder(new TitledBorder("Elevator " + n ));
		setLayout(new BorderLayout());
		add(BorderLayout.CENTER,upPanel);
		add(BorderLayout.SOUTH,openCloseButtonPanel);
		upPanel.setLayout(new GridLayout(1,2));
		upPanel.add(elevatorButtonPanel);

		
		//设置楼层数字按钮
		elevatorButtonPanel.setLayout(new GridLayout(20,1));
		for (int i = 0; i < elevatorButtons.length; i++)
		{
			elevatorButtons[i] = new ElevatorNumberButton(i);
			elevatorButtons[i].addActionListener(new ElevatorNumberButtonListener());
		}
		for (int i = elevatorButtons.length - 1; i >= 0; i--) 
		{
			elevatorButtonPanel.add(elevatorButtons[i]);
		}
		elevatorPanel = new ElevatorPanel(elevatorButtons,m_UpDownButtons);
		upPanel.add(elevatorPanel);
		//开关按钮
		openCloseButtonPanel.setLayout(new GridLayout(1,2));
		ActionListener openButtonListener = new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
//				if (elevatorPanel.getElevatorCondition())
//				{
				elevatorPanel.setIsOpen(true);
//				}
			}
		};
		ActionListener closeButtonListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e) 
			{
				elevatorPanel.setIsOpen(false);
			}
		};
		openButton.addActionListener(openButtonListener);
		closeButton.addActionListener(closeButtonListener);
		openCloseButtonPanel.add(openButton);
		openCloseButtonPanel.add(closeButton);
		
		
		//电梯开始并发检查
		ExecutorService exec = Executors.newCachedThreadPool();
		exec.execute(elevatorPanel);
		exec.shutdown();
	}
	
	
	//为ElevatorPanel扩展接口
	public void refreshElevatorObject()
	{
		elevatorPanel.refreshElevatorObject();
	}
	public int timeToFloor(int iFloor,Direction direction)
	{
		return elevatorPanel.timeToFloor(iFloor, direction);
	}
	public void setUpDownButtonCondition(Direction direction,int iFloorNumber,boolean f)
	{
		elevatorPanel.setUpDownButtonCondition(direction, iFloorNumber, f);
	}
}


class ElevatorNumberButton extends JToggleButton
{
	private int m_iFloorNumber;
	public int getFloorNumber(){return m_iFloorNumber;};
	public ElevatorNumberButton(int iFloorNumber) 
	{
		super("" + (iFloorNumber + 1));
		m_iFloorNumber = iFloorNumber;
	}
}





class ElevatorPanel extends JPanel implements Runnable
{
	
	//上下按钮面板
	private UpDownButtonPanel m_UpDownButtonPanel; 
	//楼层按钮
	private ElevatorNumberButton[] m_ElevatorNumberButtons;
	//电梯
	private JToggleButton m_ElevatorObject = null;
	
	//状态量
	private int m_iPosition;					//所在格数(每层分割为DIVIDE_NUMBER格)
	private Direction m_Direction;
	//用于记录由up或down button分配的状况
	private boolean[] upButtonCondition = new boolean[MainWindows.FLOOR_NUMBER];
	private boolean[] downButtonCondition = new boolean[MainWindows.FLOOR_NUMBER];
	public synchronized void setUpDownButtonCondition(Direction direction,int iFloorNumber,boolean f)
	{
		if (direction == Direction.DIRECTION_UP)
		{
			upButtonCondition[iFloorNumber] = f;
		}
		else if (direction == Direction.DIRECTION_DOWN)
		{
			downButtonCondition[iFloorNumber] = f;
		}
	}
	public synchronized boolean getUpDownButtonCondition(Direction direction,int iFloorNumber)
	{
		if (direction == Direction.DIRECTION_UP)
		{
			return upButtonCondition[iFloorNumber];
		}
		else 
		{
			return downButtonCondition[iFloorNumber];
		}
	}

	private boolean m_fIsOpen = false;					//记录电梯open键是否按下
	public synchronized boolean getIsOpen(){return m_fIsOpen;}
	public synchronized void setIsOpen(boolean isOpen)
	{
//		if (!checkUp() && !checkDown())
//		{
//			System.out.println("ElevatorPanel.setIsOpen()aaaaaa");
//		}
		
		if (getElevatorCondition() || (!checkUp() && !checkDown()))
		{
//			m_fIsOpen = isOpen;
//			System.out.println("ElevatorPanel.setIsOpen()");
			if (isOpen)
			{
				m_fIsOpen = isOpen;
				m_ElevatorObject.setSelected(true);
			}
			else if (m_fIsOpen)
			{
				m_fIsOpen = false;
				
				m_ElevatorObject.setSelected(false);
			}
		}
	}
	public synchronized boolean getElevatorCondition(){ return m_ElevatorObject.isSelected();}
	
	//常量
	private static int DIVIDE_NUMBER = 5;			//电梯动画每层分数
	private static final long MOVE_DELAY = 100l;	//每格停留时间 毫秒
	private static final long PAUSE_DURATION = 1000l;	//开门停留时间 毫秒

	public ElevatorPanel(ElevatorNumberButton[] elevatorNumberButtons, UpDownButtonPanel upDownButtonPanel) 
	{
		super();
		m_UpDownButtonPanel = upDownButtonPanel;
		m_ElevatorNumberButtons = elevatorNumberButtons;
		m_iPosition = 0;
		m_Direction = Direction.DIRECTION_NONE;
		setLayout(null);
		for (int i = 0; i < upButtonCondition.length; i++) 
		{
			upButtonCondition[i] = false;
			downButtonCondition[i] = false;
		}
	}
	
	public void refreshElevatorObject()		//刷新电梯位置
	{
		if (m_ElevatorObject == null) 
		{
			m_ElevatorObject = new JToggleButton();
			add(m_ElevatorObject);
			ActionListener disableSelect = new ActionListener() 
			{
				public void actionPerformed(ActionEvent e) 
				{
					JToggleButton button =(JToggleButton) e.getSource();
					if (button.isSelected())
					{
						button.setSelected(false);
					}
					else
					{
						button.setSelected(true);
					}
				}
			};
			m_ElevatorObject.addActionListener(disableSelect);
		}
		Rectangle rect = m_ElevatorNumberButtons[0].getBounds();
		rect.y += m_iPosition * 
				(m_ElevatorNumberButtons[MainWindows.FLOOR_NUMBER - 1].getBounds().y - m_ElevatorNumberButtons[0].getBounds().y)
				/ (DIVIDE_NUMBER * (MainWindows.FLOOR_NUMBER - 1));
		m_ElevatorObject.setBounds(rect);
		
	}
	
	
	//计算到达指定方向楼层的时间，用于确定电梯分配
	
	public int timeToFloor(int iFloor,Direction direction)
	{
		int iFloorNumber = iFloor * DIVIDE_NUMBER;
		if (direction == Direction.DIRECTION_NONE)
		{
			
				int i = iFloorNumber - m_iPosition;
				if (i > 0)
				{
					return i;
				}
				else
				{
					return -i;
				}
		}
		if (direction == Direction.DIRECTION_UP)		//按钮上楼
		{
			if (m_iPosition > iFloorNumber) 
			{
				if (m_Direction == Direction.DIRECTION_UP) 
				{
					return (2 * (MainWindows.FLOOR_NUMBER - 1) * DIVIDE_NUMBER - m_iPosition - iFloorNumber);
				}
				else if (m_Direction == Direction.DIRECTION_DOWN)
				{
					return (m_iPosition + iFloorNumber);
				}
				else 
				{
					return (m_iPosition - iFloorNumber);
				}
			}
			else if (m_iPosition == iFloorNumber) 
			{
				if (m_Direction == Direction.DIRECTION_DOWN)
				{
					return (2 * m_iPosition); 
				}
				else
				{
					return 0;
				}
			}
			else
			{
				if (m_Direction == Direction.DIRECTION_DOWN)
				{
					return (iFloorNumber + m_iPosition);
				}
				else
				{
					return (iFloorNumber - m_iPosition);
				}
			}
		}
		else 		//按钮下楼
		{
			if (m_iPosition > iFloorNumber)
			{
				if (m_Direction == Direction.DIRECTION_UP)
				{
					return (2 * (MainWindows.FLOOR_NUMBER - 1) * DIVIDE_NUMBER - m_iPosition - iFloorNumber);
				}
				else
				{
					return (m_iPosition - iFloorNumber);
				}
			}
			else if (m_iPosition == iFloorNumber)
			{
				if (m_Direction == Direction.DIRECTION_UP)
				{
					return (2 * (MainWindows.FLOOR_NUMBER - 1 ) * DIVIDE_NUMBER - 2 * m_iPosition);
				}
				else 
				{
					return 0;
				}
			}
			else
			{
				if (m_Direction == Direction.DIRECTION_DOWN)
				{
					return (m_iPosition + iFloorNumber);
				}
				else if (m_Direction == Direction.DIRECTION_UP)
				{
					return (2 *( MainWindows.FLOOR_NUMBER - 1) * DIVIDE_NUMBER - 2 * m_iPosition);
				}
				else
				{
					return (iFloorNumber - m_iPosition);
				}
			}
		}
		
	}
	
	
	
	public void run()
	{
		while (true)
		{
			if (getIsOpen()) 
			{
				try {
					TimeUnit.MILLISECONDS.sleep(MOVE_DELAY);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
			//数字按钮上移
			if (m_Direction == Direction.DIRECTION_NONE)	//电梯为静止状态 
			{
				if (checkUp())
				{
					m_Direction = Direction.DIRECTION_UP;
					++m_iPosition;
					refreshElevatorObject();
				}
				else if (checkDown())
				{
					--m_iPosition;
					refreshElevatorObject();
					m_Direction = Direction.DIRECTION_DOWN;
				}
				else
				{
					m_Direction = Direction.DIRECTION_NONE;
				}
			}
			else if (m_Direction == Direction.DIRECTION_UP)		//电梯方向向上
			{
				if (checkUp())
				{	
					++m_iPosition;
					refreshElevatorObject();
				}
				else if (checkDown())
				{
					--m_iPosition;
					refreshElevatorObject();
					m_Direction = Direction.DIRECTION_DOWN;
				}
				else
				{
					m_Direction = Direction.DIRECTION_NONE;
				}
			}
			else												//电梯方向向下
			{
				if (checkDown())
				{	
					--m_iPosition;
					refreshElevatorObject();
				}
				else if (checkUp())
				{
					++m_iPosition;
					refreshElevatorObject();
					m_Direction = Direction.DIRECTION_UP;
				}
				else
				{
					m_Direction = Direction.DIRECTION_NONE;
				}
			}
			if (checkFloor())
			{
				m_ElevatorObject.setSelected(true);
				try {
					TimeUnit.MILLISECONDS.sleep(PAUSE_DURATION);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (!getIsOpen())
				{
					m_ElevatorObject.setSelected(false);
				}
			}
			else
			{
				try {
					TimeUnit.MILLISECONDS.sleep(MOVE_DELAY);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	private boolean checkUp()		//向上检测电梯,若有按键则电梯上移并返回true
	{
		int i = 0;
		for (i = m_iPosition / DIVIDE_NUMBER + 1; i < MainWindows.FLOOR_NUMBER; i++)
		{
			if (m_ElevatorNumberButtons[i].isSelected())
			{
				break;
			}
		}
		if (i < MainWindows.FLOOR_NUMBER)
		{
//			++m_iPosition;
//			refreshElevatorObject();
			return true;
		}
		else
		{
			for (i = m_iPosition / DIVIDE_NUMBER + 1; i < MainWindows.FLOOR_NUMBER; i++)
			{
				if (getUpDownButtonCondition(Direction.DIRECTION_UP, i))
				{
					break;
				}
			}
			if (i < MainWindows.FLOOR_NUMBER)
			{
//				++m_iPosition;
//				refreshElevatorObject();
				return true;
			}
			else
			{
				for (i = m_iPosition / DIVIDE_NUMBER + 1; i < MainWindows.FLOOR_NUMBER; i++)
				{
					if (getUpDownButtonCondition(Direction.DIRECTION_DOWN, i))
					{
						break;
					}
				}
				if (i < MainWindows.FLOOR_NUMBER)
				{
//					++m_iPosition;
//					refreshElevatorObject();
					return true;
				}
				else
				{
					return false;
				}
			}
		}
		
	}
	
	private boolean checkDown()		//向下检测电梯,若有按键则电梯下移并返回ture
	{
		int i = 0;
		if (m_iPosition % DIVIDE_NUMBER == 0)
		{
			i = m_iPosition / DIVIDE_NUMBER - 1;
		}
		else
		{
			i = m_iPosition / DIVIDE_NUMBER ;
		}
		
		for (; i >= 0; i--)
		{
			if (m_ElevatorNumberButtons[i].isSelected())
			{
				break;
			}
		}
		if (i >= 0)
		{
//			--m_iPosition;
//			refreshElevatorObject();
			return true;
		}
		else
		{
			if (m_iPosition % DIVIDE_NUMBER == 0)
			{
				i = m_iPosition / DIVIDE_NUMBER - 1;
			}
			else
			{
				i = m_iPosition / DIVIDE_NUMBER ;
			}
		
			for (; i >= 0; i--)
			{
				if (getUpDownButtonCondition(Direction.DIRECTION_DOWN, i))
				{
					break;
				}
			}
			if (i >= 0)
			{
//				--m_iPosition;
//				refreshElevatorObject();
				return true;
			}
			else
			{
				if (m_iPosition % DIVIDE_NUMBER == 0)
				{
					i = m_iPosition / DIVIDE_NUMBER - 1;
				}
				else
				{
					i = m_iPosition / DIVIDE_NUMBER ;
				}
			
				for (; i >= 0; i--)
				{
					if (getUpDownButtonCondition(Direction.DIRECTION_UP, i))
					{
						break;
					}
				}
				if (i >= 0)
				{
//					--m_iPosition;
//					refreshElevatorObject();
					return true;
				}
				else
				{
					return false;
				}
			}
		}
	}
	
	
	private boolean checkFloor()		//检测电梯所在层是否需要停
	{
		boolean f = false;
		if (m_iPosition % DIVIDE_NUMBER == 0)
		{
			int i = m_iPosition / DIVIDE_NUMBER;
			if (m_ElevatorNumberButtons[i].isSelected())
			{
				m_ElevatorNumberButtons[i].setSelected(false);
				f = true;
			}
			
			///////////
			if (m_Direction == Direction.DIRECTION_UP) 
			{
				if (getUpDownButtonCondition(Direction.DIRECTION_UP, i))
//				if (m_UpDownButtonPanel.getButtonCondition(Direction.DIRECTION_UP, i))
				{
					//setUpDownButtonCondition(Direction.DIRECTION_UP, i, false);
					m_UpDownButtonPanel.resignUpDownButton(Direction.DIRECTION_UP, i);
					f = true;
				}
				if (!checkUp() && getUpDownButtonCondition(Direction.DIRECTION_DOWN, i) )
//				if (!checkUp() && m_UpDownButtonPanel.getButtonCondition(Direction.DIRECTION_DOWN, i))
				{
					//setUpDownButtonCondition(Direction.DIRECTION_DOWN, i, false);
					m_UpDownButtonPanel.resignUpDownButton(Direction.DIRECTION_DOWN, i);
					m_Direction = Direction.DIRECTION_DOWN;
					f = true;
				}
			}
			else if (m_Direction == Direction.DIRECTION_DOWN) 
			{
				if (getUpDownButtonCondition(Direction.DIRECTION_DOWN, i))
//				if (m_UpDownButtonPanel.getButtonCondition(Direction.DIRECTION_DOWN, i))
				{
					//setUpDownButtonCondition(Direction.DIRECTION_DOWN, i, false);
					m_UpDownButtonPanel.resignUpDownButton(Direction.DIRECTION_DOWN, i);
					f = true;
				}
				if (!checkDown() && getUpDownButtonCondition(Direction.DIRECTION_UP, i) )
//				if (!checkDown() && m_UpDownButtonPanel.getButtonCondition(Direction.DIRECTION_UP, i))
				{
					//setUpDownButtonCondition(Direction.DIRECTION_UP, i, false);
					m_UpDownButtonPanel.resignUpDownButton(Direction.DIRECTION_UP, i);
					m_Direction = Direction.DIRECTION_UP;
					f = true;
				}
			}
			else
			{
				if (getUpDownButtonCondition(Direction.DIRECTION_UP, i) )
//				if (m_UpDownButtonPanel.getButtonCondition(Direction.DIRECTION_UP, i))
				{
					//setUpDownButtonCondition(Direction.DIRECTION_UP, i, false);
					m_Direction = Direction.DIRECTION_UP;
					m_UpDownButtonPanel.resignUpDownButton(Direction.DIRECTION_UP, i);
					f = true;
				}
				else if (getUpDownButtonCondition(Direction.DIRECTION_DOWN, i) )
//				else if (m_UpDownButtonPanel.getButtonCondition(Direction.DIRECTION_DOWN, i))
				{
					//setUpDownButtonCondition(Direction.DIRECTION_DOWN, i, false);
					m_Direction = Direction.DIRECTION_DOWN;
					m_UpDownButtonPanel.resignUpDownButton(Direction.DIRECTION_DOWN, i);
					f = true;
				}
			}
		}
		return f;
	}
}


class ElevatorNumberButtonListener implements ActionListener
{
	public ElevatorNumberButtonListener() 
	{
	}
	public void actionPerformed(ActionEvent event)
	{
		ElevatorNumberButton button = (ElevatorNumberButton)event.getSource();
		//isSelected返回点击前状态
		if (button.isSelected())
		{
			
		}
		else
		{
			//电梯无法取消按钮
			//拒绝按钮取消
			button.setSelected(true);
		}
	}
}
