import java.awt.GridLayout;

import javax.swing.JFrame;

public class MainWindows extends JFrame
{
	public static final int FLOOR_NUMBER = 20;		//����¥����
	static final int ELEVATOR_NUMBER = 5;
	private UpDownButtonPanel upDownButtonPanel;
	private Elevator[] elevators = new Elevator[FLOOR_NUMBER];
	public MainWindows() 
	{
		super();
		//���֣�1�����°�ť����5��������
		setLayout(new GridLayout(1,6));
		
		//���������밴ť���������
		//�����������밴ť�����໥����
		upDownButtonPanel = new UpDownButtonPanel();
		for (int i = 0; i < elevators.length; i++) 
		{
			elevators[i] = new Elevator(i + 1,upDownButtonPanel);
		}
		upDownButtonPanel.setElevators(elevators);

		add(upDownButtonPanel);
		for (int i = 0; i < ELEVATOR_NUMBER; i++)
		{
			add(elevators[i]);
		}

		//���ô�����������
		setTitle("elevator  by 1152822_wuxiangyu");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(800, 600);
		setVisible(true);
		
		pack();
		for (int i = 0; i < ELEVATOR_NUMBER; i++) 
		{
			elevators[i].refreshElevatorObject();
		}
	}
}



