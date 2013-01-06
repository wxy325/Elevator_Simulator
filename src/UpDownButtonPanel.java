//UpDownButtonPanel.java
//实现电梯楼层上下按钮


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class UpDownButtonPanel extends JPanel
{
	//界面相关data member
	private UpDownButton[] upButtons = new UpDownButton[MainWindows.FLOOR_NUMBER];
	
	private UpDownButton[] downButtons = new UpDownButton[MainWindows.FLOOR_NUMBER];
	public synchronized boolean getButtonCondition(Direction direction, int n)
	{
		if (direction == Direction.DIRECTION_UP)
		{
			return upButtons[n].isSelected();
		}
		else
		{
			return downButtons[n].isSelected();
		}
	}
	public synchronized void setButtonCondition(Direction direction,int n, boolean f)
	{
		if (direction == Direction.DIRECTION_UP)
		{
			upButtons[n].setSelected(f);
		}
		else if (direction == Direction.DIRECTION_DOWN)
		{
			downButtons[n].setSelected(f);
		}
		
	}
	public synchronized void resignUpDownButton(Direction direction,int n)
	{
		setButtonCondition(direction, n, false);
		Elevator elevator =  getUpDownDispatchElevators(direction, n);
		elevator.setUpDownButtonCondition(direction, n, false);
		setUpDownDispatchElevators(direction, n, null);
	}
	
	//状态记录 data member	需多线程访问的变量都设置了synchronized的get与set方法
	private Elevator[] m_rgUpDispatchElevators = new Elevator[MainWindows.FLOOR_NUMBER];	//记录按钮所分配的电梯	
	private Elevator[] m_rgDownDispatchElevators = new Elevator[MainWindows.FLOOR_NUMBER];
	public synchronized Elevator getUpDownDispatchElevators(Direction direction,int n)
	{
		if (direction == Direction.DIRECTION_UP)
		{
			return m_rgUpDispatchElevators[n];
		}
		else
		{
			return m_rgDownDispatchElevators[n];
		}
	}
	public synchronized void setUpDownDispatchElevators(Direction direction,int n,Elevator elevator)
	{
		if (direction == Direction.DIRECTION_UP) 
		{
			m_rgUpDispatchElevators[n] = elevator;
		}
		else if (direction == Direction.DIRECTION_DOWN)
		{
			m_rgDownDispatchElevators[n] = elevator;
		}
	}
	
	private Elevator[] m_rgElevators;
	public synchronized void setElevators(Elevator[] rgElevators)
	{
		m_rgElevators = rgElevators;
		for (int i = 0; i < upButtons.length; i++) 
		{
			upButtons[i].setElevators(rgElevators);
			downButtons[i].setElevators(rgElevators);
		}
	}
	public synchronized Elevator getElevator(int n){return m_rgElevators[n];};
	
	//member function
	public UpDownButtonPanel()
	{
		super();
		
		//状态变量初始化
		m_rgElevators = null;
		for (int i = 0; i < MainWindows.FLOOR_NUMBER; i++) 
		{
			setUpDownDispatchElevators(Direction.DIRECTION_UP, i, null);
			setUpDownDispatchElevators(Direction.DIRECTION_DOWN, i, null);
		}
		
		//界面初始化
		setBorder(new TitledBorder("Button"));
		setLayout(new BorderLayout());
		JPanel buttonPanel = new JPanel();
		add(BorderLayout.CENTER,buttonPanel);
		buttonPanel.setLayout(new GridLayout(20,2));
		
		JPanel tempPanel = new JPanel();
		tempPanel.setLayout(new GridLayout(1,2));
		
		////此部分按钮无实际作用，仅用于调整屏幕布局
		JButton tempButton = new JButton();
		tempButton.setVisible(false);
		JButton tempButton2 = new JButton();
		tempButton2.setVisible(false);
		tempPanel.add(tempButton);
		tempPanel.add(tempButton2);
		add(BorderLayout.SOUTH,tempPanel);
		/////
		//添加按钮，顺序为从19到0
		for (int i = upButtons.length - 1; i >= 0; i--)
		{
			upButtons[i] = new UpDownButton(Direction.DIRECTION_UP,i,this);
			downButtons[i] = new UpDownButton(Direction.DIRECTION_DOWN,i,this);
			upButtons[i].setVisible(true);
			downButtons[i].setVisible(true);
			upButtons[i].addActionListener(new UpDownButtonListener());
			downButtons[i].addActionListener(new UpDownButtonListener());
			buttonPanel.add(upButtons[i]);
			buttonPanel.add(downButtons[i]);
		}
		upButtons[downButtons.length - 1].setVisible(false);
		downButtons[0].setVisible(false);
	}
	
	
	
	//按钮响应
	class UpDownButtonListener implements ActionListener
	{
		public UpDownButtonListener() 
		{
		}
		
		public void actionPerformed(ActionEvent event)
		{
			UpDownButton button = (UpDownButton)event.getSource();
			if (button.isSelected())
			{
//				button.setEnabled(false);
				//计算时间,分配电梯
				
				int iFloorNumber = button.getFloorNumber();
				Direction direction = button.getDirection();
				Direction oppositeDirection;
				if (direction == Direction.DIRECTION_UP)
				{
					oppositeDirection = Direction.DIRECTION_DOWN;
				}
				else
				{
					oppositeDirection = Direction.DIRECTION_UP;
				}
				
				Elevator elevator = button.getElevator(0);
				
				for (int i = 0; i < MainWindows.ELEVATOR_NUMBER; i++) 
				{
					if (getUpDownDispatchElevators(oppositeDirection, iFloorNumber) == elevator)	//防止同一楼层的上下键分配到同一部电梯
					{
						elevator = button.getElevator(i + 1);
						continue;
					}
					if (button.getElevator(i).timeToFloor(iFloorNumber, direction) < elevator.timeToFloor(iFloorNumber, direction) )	//计算最短时间 
					{
//						if (getUpDownDispatchElevators(oppositeDirection, iFloorNumber) != elevator)	
//						{
							elevator = button.getElevator(i);
//						}
					}
				}
				//elevator为最近电梯
				elevator.setUpDownButtonCondition(direction, iFloorNumber, true);
				button.getUpDownButtonPanel().setUpDownDispatchElevators(direction, iFloorNumber, elevator);
			}
			else
			{
				//电梯无法取消按钮
				//拒绝按钮取消
				button.setSelected(true);
			}
		}
	}
	

	
	
}


class UpDownButton extends JToggleButton
{
	private int m_iFloorNumber;
	public int getFloorNumber(){return m_iFloorNumber;};

	private Elevator[] m_rgElevators;
	public synchronized void setElevators(Elevator[] rgElevators){m_rgElevators = rgElevators;};
	public synchronized Elevator getElevator(int n){return m_rgElevators[n];};
	
	private UpDownButtonPanel m_UpDownButtonPanel;
	public UpDownButtonPanel getUpDownButtonPanel(){return m_UpDownButtonPanel;};
	
	private Direction m_Direction;
	Direction getDirection(){return m_Direction;}
	public UpDownButton(Direction direction,int floorNumber,UpDownButtonPanel upDownButtonPanel)
	{
		super();
		
		if (direction == Direction.DIRECTION_UP)
		{
			super.setText("Up");
		}
		else
		{
			super.setText("Down");
		}
		m_iFloorNumber = floorNumber;
		m_Direction = direction;
		m_UpDownButtonPanel = upDownButtonPanel;
	}
	
}




