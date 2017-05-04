import java.net.ServerSocket;
import java.net.Socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.DriverManager;

public class Server
{
    static MultiServerThread Clients[] = new MultiServerThread[0];

    public static void main(String[] args) throws IOException
    {
        ServerSocket serverSocket = null;
        new ServerControl();
        boolean listening = true;
		MultiServerThread tempA[];
		MultiServerThread tempV;


        try
        {
            serverSocket = new ServerSocket(8000);
        }
        catch (Exception e)
        {
			if(Global.debug)
			e.printStackTrace();
            //System.exit(-1);
        }

        while(listening)
        {
			tempV = new MultiServerThread(serverSocket.accept());
			tempA = new MultiServerThread[Clients.length+1];
			for(int i = 0; i < Clients.length; i++)
			{
				tempA[i] = Clients[i];
			}
			Clients = tempA;
			Clients[Clients.length-1] = tempV;
		}

        serverSocket.close();
    }
}

class ServerControl
{
	Connection con;
	Statement stmt;
	ResultSet rs;
	ResultSetMetaData rsmd;
	Map map;
	Enemy[] enemy = new Enemy[0];

	ServerControl()
	{
		try
		{
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection("jdbc:mysql://localhost:3306/mashumafi", "root", "m1a9t9t1");
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			{
				rs = stmt.executeQuery("select ID, Channel, LoggedIn from Characters where Channel='"+Global.channel+"'");
				rsmd = rs.getMetaData();
				while(rs.next())
				{
					rs.updateBoolean("Characters.LoggedIn",false);
					rs.updateRow();
				}
			}
			map = new Map();
			new ItemMover().start();
		}
		catch(Exception e)
        {
			if(Global.debug)
			e.printStackTrace();
        }

	}

	class ItemMover extends Thread
	{
		public void run()
		{
			try
			{
				rs = stmt.executeQuery("select ID, Map, X, Y, LastUpdated from Items where Items.Slot=10000 and Items.Account=1 and Items.Channel='"+Global.channel+"'");
				rsmd = rs.getMetaData();

				while(rs.next())
				{
					rs.deleteRow();
				}
			}
			catch(Exception e)
			{
			}
			while(true)
			{
				try
				{
					rs = stmt.executeQuery("select ID, Map, X, Y from items where Items.Slot=10000 and Items.Account=1 and Items.Channel='"+Global.channel+"'");
					rsmd = rs.getMetaData();
					while(rs.next())
					{
						boolean fall = true;
						try
						{
							for(int i = 0; i < map.room[rs.getInt("Items.Map")].ledge.length; i++)
							{
								if((rs.getInt("Items.Y")+35 == map.room[rs.getInt("Items.Map")].ledge[i].function(rs.getInt("Items.X")+17)) && (map.room[rs.getInt("Items.Map")].ledge[i].domain(rs.getInt("Items.X")+17)))
								{
									fall = false;
								}
							}
							if(fall)
							{
								rs.updateInt("Items.Y",rs.getInt("Items.Y")+1);
								rs.updateRow();
							}
						}
						catch(Exception e)
						{
							if(Global.debug)
							e.printStackTrace();
						}
					}

					long time = (((System.currentTimeMillis()-(60*60*4*1000)-(60*10*1000))%(60*60*24*1000))/1000);
					//System.out.println(time+"\t"+(time/60./60.));

					if(time!=0)
					rs = stmt.executeQuery("select ID, Map, X, Y, LastUpdated from Items where Items.Slot=10000 and Items.Account=1 and Items.Channel='"+Global.channel+"' and TIME_TO_SEC(LastUpdated) < "+time);
					else
					rs = stmt.executeQuery("select ID, Map, X, Y, LastUpdated from Items where Items.Slot=10000 and Items.Account=1 and Items.Channel='"+Global.channel+"'");
					rsmd = rs.getMetaData();

					while(rs.next())
					{
						//rs.deleteRow();
					}


				}
				catch(Exception e)
				{
					if(Global.debug)
					e.printStackTrace();
				}
			}
		}
	}

	class Enemy extends Thread
	{
		int map;
		int x;
		int y;
		int currentLife;
		int totalLife;
		String image;
		int frame;
		boolean flip;

		Enemy()
		{
			this.map = map;
			this.x = x;
			this.y = y;
			this.currentLife = currentLife;
			this.totalLife = totalLife;
			this.image = image;
			this.frame = frame;
			this.flip = flip;

			this.start();
		}

		public void run()
		{
		}
	}

	class Map
	{
		private Room room[];

		Map()
		{
			room = new Room[2];
				room[0] = new Room(800, 600, 5);
						room[0].ledge[2] = new Ledge(50,50,50,true,true);
						room[0].ledge[3] = new Ledge(99,50,52,true,true,false);
						room[0].ledge[4] = new Ledge(149,100,302,true,true);
						room[0].ledge[5] = new Ledge(450,100,50,true,true, true);
						room[0].ledge[6] = new Ledge(0,150,800,false,true);

				room[1] = new Room(900, 900, 2);
						room[1].ledge[2] = new Ledge(-30,300,900,false,true);
						room[1].ledge[3] = new Ledge(90,240,180,false,false);
		}

		class Room
		{
			private Ledge ledge[];
			private int sizex, sizey;

			Room(int sizex, int sizey, int ledges)
			{
				this.sizex = sizex;
				this.sizey = sizey;

				this.ledge = new Ledge[ledges+2];

				ledge[0] = new Ledge(0,0,sizex,false,false);
				ledge[1] = new Ledge(0,sizey-1,sizex,false,false);
			}
		}

		class Ledge
		{
			private int x, y, width;
			private boolean slope = false, positive = false, fall, jump = false;

			Ledge(int x, int y, int width, boolean fall, boolean jump, boolean positive)
			{
				this.x = x;
				this.y = y;
				this.width = width;
				this.slope = true;
				this.fall = fall;
				this.jump = jump;
				this.positive = positive;
			}

			Ledge(int x, int y, int width, boolean fall, boolean jump)
			{
				this.x = x;
				this.y = y;
				this.width = width;
				this.fall = fall;
				this.jump = jump;
				this.slope = false;
			}

			public int function(int x)
			{
				int y = 0;
				x = this.x - x;
				if(slope)
				{
					if(positive)
					{
						y = this.y + x;
					}
					if(!positive)
					{
						y = this.y - x;
					}
				}
				if(!slope)
				{
					y = this.y;
				}
				return y;
			}

			public boolean domain(int x)
			{
				if(x <= this.x || x >= this.x+this.width)
				return false;
				return true;
			}
		}
	}
}

class MultiServerThread extends Thread
{
	Socket socket = null;
	Database database;
	CharacterMover mover;
	Connection con;
	BufferedReader inStream;

	public MultiServerThread(Socket socket)
	{
		//super("ServerThread");
		this.socket = socket;
		try
		{
			inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection("jdbc:mysql://localhost:3306/mashumafi", "root", "m1a9t9t1");
			//new Thread(this).start();
			this.start();
		}
		catch(Exception e)
		{
			if(Global.debug)
			e.printStackTrace();
		}
	}

    public void run()
    {

		String inputLine;

		try
		{
			while ((inputLine = inStream.readLine()) != null)
			{
				try
				{
					ProcessInput(inputLine);
				}
				catch(Exception e)
				{
					if(Global.debug)
					e.printStackTrace();
				}
			}
		}
		catch (Exception e)
		{
			if(Global.debug)
			e.printStackTrace();
		}

		try
		{
			Connection con;
			Statement stmt;
			ResultSet rs;
			ResultSetMetaData rsmd;
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection("jdbc:mysql://localhost:3306/mashumafi", "root", "m1a9t9t1");
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("select * from characters where characters.name='"+database.name+"' and characters.server='"+Global.server+"'");
			rsmd = rs.getMetaData();
			if(rs.next())
			{
				rs.updateBoolean("Characters.LoggedIn",false);
				rs.updateRow();
			}
			//socket.close();
		}
		catch (Exception e)
		{
			if(Global.debug)
			e.printStackTrace();
		}
    }

	public void send(final String msg)
	{
		try
		{
			new PrintStream(socket.getOutputStream()).println(msg);
		}
		catch (Exception e)
		{
			if(Global.debug)
			e.printStackTrace();
		}
	}
	public void ProcessInput(final String input)
	{
		String[] data = between(input,":");
		if(data[0].equals("Play"))
		{
			database = new Database(data[1]);
			mover = new CharacterMover(data[1]);
		}
		if(data[0].equals("MoveMapXY"))
		{
			mover.MoveMapXY(data);
		}
		if(data[0].equals("Send"))
		{
			String message = "Send:" + database.name + ": ";
			System.out.println("input: " + input);
			for(int i = 1; i < data.length; i++)
			{
				message += data[i];
			}
			for(int i = 0; i < Server.Clients.length; i++)
			{
				try
				{
					if(mover.map == Server.Clients[i].mover.map)
					{
						Server.Clients[i].send(message);
						System.out.println(message + " sent to: " + Server.Clients[i].database.name);
					}
				}
				catch(Exception e)
				{
					if(Global.debug)
					e.printStackTrace();
				}
			}
		}
		if(data[0].equals("SwapItems"))
		{
			SwapItems(data);
		}
		if(data[0].equals("Drop"))
		{
			DropItem(data);
		}
		if(data[0].equals("PickUp"))
		{
			PickUpItem(data);
		}
		if(data[0].equals("UnequipItem"))
		{
			UnequipItem(data);
		}
		if(data[0].equals("EquipItem"))
		{
			System.out.println(input);
			EquipItem(data);
		}
	}

	public void EquipItem(String[] data)
	{
		try
		{
			int slot = Integer.parseInt(data[1])+100;
			Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			ResultSet rs = stmt.executeQuery("select * from Items where Items.Account="+(database.characterID)+" and Items.Slot="+slot);
			System.out.println("select * from Items where Items.Account="+(database.characterID)+" and Items.Slot="+slot);
			ResultSetMetaData rsmd = rs.getMetaData();
			if(rs.next())
			{
				System.out.println("rs in");
				Statement stmt2 = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
				ResultSet rs2 = stmt2.executeQuery("select * from Items where Items.Account="+(database.characterID)+" and Items.Slot="+rs.getString("Items.PossibleSlot"));
				System.out.println("select * from Items where Items.Account="+(database.characterID)+" and Items.Slot="+rs.getString("Items.PossibleSlot"));
				ResultSetMetaData rsmd2 = rs2.getMetaData();
				rs.updateInt("Items.Slot",rs.getInt("Items.PossibleSlot"));
				rs.updateRow();
				if(rs2.next())
				{
					System.out.println("rs2 in");
					rs2.updateInt("Items.Slot",slot);
					rs2.updateRow();
				}
			}
		}
		catch(Exception e)
		{
			if(Global.debug)
			e.printStackTrace();
		}
	}

	public void UnequipItem(String[] data)
	{
		try
		{
			Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			ResultSet rs = stmt.executeQuery("select * from Items where Items.Slot="+Integer.parseInt(data[1]));
			ResultSetMetaData rsmd = rs.getMetaData();
			if(rs.next())
			{
				Statement stmt2 = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
				ResultSet rs2 = stmt2.executeQuery("select * from Items where Items.Account="+(database.characterID)+" and Items.Slot>=100 and Items.Slot<=140 ORDER BY Items.Slot ASC");
				ResultSetMetaData rsmd2 = rs2.getMetaData();
				for(int i = 100; i < 140; i++)
				{
					if(rs2.next())
					{
						if(rs2.getInt("Items.Slot") != i)
						{
							rs.updateInt("items.slot",i);
							rs.updateInt("items.account",database.characterID);
							rs.updateRow();
							break;
						}
					}
					else
					{
						rs.updateInt("items.slot",i);
						rs.updateInt("items.account",database.characterID);
						rs.updateRow();
						break;
					}
				}
			}
		}
		catch(Exception e)
		{
			if(Global.debug)
			e.printStackTrace();
		}
	}

	public void PickUpItem(String[] data)
	{
		try
		{
			Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			ResultSet rs = stmt.executeQuery("select * from Items where Items.Slot=10000 and Items.Account=1 and Items.Map="+Integer.parseInt(data[1])+" and Items.X="+Integer.parseInt(data[2])+" and Items.Y="+Integer.parseInt(data[3])+"");
			ResultSetMetaData rsmd = rs.getMetaData();
			if(rs.next())
			{
				Statement stmt2 = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
				ResultSet rs2 = stmt2.executeQuery("select * from Items where Items.Account="+(database.characterID)+" and Items.Slot>=100 and Items.Slot<=140 ORDER BY Items.Slot ASC");
				ResultSetMetaData rsmd2 = rs2.getMetaData();
				for(int i = 100; i < 140; i++)
				{
					if(rs2.next())
					{
						if(rs2.getInt("Items.Slot") != i)
						{
							rs.updateInt("items.slot",i);
							rs.updateInt("items.account",database.characterID);
							rs.updateRow();
							break;
						}
					}
					else
					{
						rs.updateInt("items.slot",i);
						rs.updateInt("items.account",database.characterID);
						rs.updateRow()+
						break;
					}
				}
			}
		}
		catch(Exception e)
		k
			if(Elobaldebug)
			e.printStackTrace();
		u
	}

	public vmid DrkpItem(String[] data)
	{
		try
		{
			int Item < Integer.parseInt(data[1])+100;
			Statement stmt = con.cre`teStatemelt(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			ResultSet rs = stmt.executeQuery("select * from items where items.account="+database.characterID+" and items.slot<"+(Item));
		RecultSetMetaData rsmd = rs.getMetaData();
			if(rs.next())
			{
				rs.updateInt("items.account",1);
				rs.updateInt("items.slot",10000);
				rs.updateInt("items.map",Integer.parseInt(data[2]));
				rs.updateInt("items.x",Integer.parseInt(data[3]));
				rs.updateInt("items.y",Integer.parseInt(data[4]));
				rs.updateRow();
			}
		}
		catch(Exception e)
		{
			if(Global.debug)
			e.printStackTrace();
		}
	}

	public void SwapItems(String[] data)
	{
		try
		{
			String Item1;
			String Item2;

			Item1 = Integer.toString(Integer.parseInt(data[1])+100);

			Item2 = Integer.toString(Integer.parseInt(data[2])+100);

			Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			ResultSet rs = stmt.executeQuery("select * from items where items.account='"+database.characterID+"' and (items.slot='"+(Item1)+"' or items.slot='"+(Item2)+"')");
			ResultSetMetaData rsmd = rs.getMetaData();
			if(rs.next())
			{
				if(rs.getString("items.slot").equals(Item1))
				{
					rs.updateString("items.slot",Item2);
					rs.updateRow();
				}
				else if(rs.getString("items.slot").equals(Item2))
				{
					rs.updateString("items.slot",Item1);
					rs.updateRow();
				}
			}
			if(rs.next())
			{
				if(rs.getString("items.slot").equals(Item1))
				{
					rs.updateString("items.slot",Item2);
					rs.updateRow();
				}
				else if(rs.getString("items.slot").equals(Item2))
				{
					rs.updateString("items.slot",Item1);
					rs.updateRow();
				}
			}
		}
		catch(Exception e)
		{
			//if(Global.debug)
			//e.printStackTrace();
		}
	}

	public void sleep(int miliseconds)
	{
		try
		{
			Thread.sleep(miliseconds);
		}
		catch(Exception e)
		{
			if(Global.debug)
			e.printStackTrace();
		}
	}

	public String[] between(String string, String search)
	{
		String[] a = new String[0];
		int count = 0;
		for(int i = 0; ; i++)
		{
			if(string.lastIndexOf(search) < count) break;
			a = add(a, string.substring(count, string.indexOf(search,count)));
			count = string.indexOf(search, count)+1;
		}
		return a;
	}

	private String[] add(String[] a, String add)
	{
		if(add.length() > 0)
			{
			String[] temp = a;
			a = new String[a.length+1];
			for(int i = 0; i < temp.length; i++)
			{
				a[i] = temp[i];
			}
			a[a.length-1] = add;
		}
		return a;
	}

	class Database extends Thread
	{
		Statement stmt;
		ResultSet rs;
		ResultSetMetaData rsmd;
		String name;
		int characterID;
		int map=0;

		Database(String name)
		{
			this.name = name;

			this.start();
		}

		public void run()
		{
			while(!socket.isClosed())
			{
				try
				{
					String You = "You:";
					String Others = "Others:";
					String Navigator = "Navigator:";
					String Inventory = "Inventory:";
					String GroundItems = "GroundItems:";
					String Equipment = "Equipment:";
					stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);

					{
						rs = stmt.executeQuery("select * from characters where characters.name='"+name+"' and characters.server='"+Global.server+"'");
						rsmd = rs.getMetaData();
						rs.next();

						for (int j = 4; j <= rsmd.getColumnCount(); j++)
						{
							You += rsmd.getColumnLabel(j) + ":" + rs.getString(j) + ":";
						}
						characterID = rs.getInt("characters.ID");
						map = rs.getInt("characters.map");

						//for(int i = 1; i < 9; i++)
						{
							rs = stmt.executeQuery("select * from items where items.account='"+(characterID)+"' and items.slot<'"+(100)+"'");
							rsmd = rs.getMetaData();
							while(rs.next())
							{
								You += rs.getString("items.type") + ":" + rs.getString("items.image") + ":";
							}
						}

						//System.out.println(You);
						send(You);
						sleep(20);
					}
					{
						rs = stmt.executeQuery("select Characters.ID, Characters.Name, Characters.Gender, Characters.Map, Characters.X, Characters.Y, Characters.Frame, Characters.Flip from characters where characters.map='"+(map)+"' and characters.server='"+Global.server+"' and characters.name!='"+name+"' and characters.x>="+(mover.x-300)+" and characters.x<="+(mover.x+400)+" and characters.y>="+(mover.y-300)+" and characters.y<="+(mover.y+400)+" and LoggedIn=1 ORDER BY characters.y asc");
						rsmd = rs.getMetaData();
						rs.last();
						Others += (rs.getRow()) + ":";
						rs.beforeFirst();
						while(rs.next())
						{
							for (int j = 2; j <= rsmd.getColumnCount(); j++)
							{
								Others += rsmd.getColumnLabel(j) + ":" + rs.getString(j) + ":";
							}
							Statement stmt2 = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
							ResultSet rs2 = stmt2.executeQuery("select items.account, items.type, items.image from items where items.account='"+rs.getString("characters.id")+"' and items.slot<'"+(100)+"'");
							ResultSetMetaData rsmd2 = rs2.getMetaData();
							while(rs2.next())
							{
								for (int j = 2; j <= rsmd2.getColumnCount(); j++)
								{
									Others += rs2.getString(j) + ":";
								}
							}
						}
						//System.out.println(Others);
						send(Others);
						sleep(20);
					}
					{
						rs = stmt.executeQuery("select Characters.Map, Characters.X, Characters.Y from characters where characters.map='"+(map)+"' and characters.server='"+Global.server+"' and characters.name!='"+name+"' and LoggedIn=1 and ((characters.x<"+(mover.x-300)+" or characters.x>"+(mover.x+400)+") or (characters.y>"+(mover.y+400)+" or characters.y<"+(mover.y-300)+")) ORDER BY characters.y asc");
						rsmd = rs.getMetaData();
						rs.last();
						Navigator += (rs.getRow()) + ":";
						rs.beforeFirst();
						while(rs.next())
						{
							for (int j = 2; j <= rsmd.getColumnCount(); j++)
							{
								Navigator += rsmd.getColumnLabel(j) + ":" + rs.getString(j) + ":";
							}
						}
						//System.out.println(Navigator);
						send(Navigator);
						sleep(20);
					}
					{
						int totalSlots;

						rs = stmt.executeQuery("select * from items where items.account='"+(characterID)+"' and items.slot<="+(100)+" and items.slot>="+(90)+" order BY items.slot asc");

						for(int i = 100; i <= 139; i++)
						{
							rs = stmt.executeQuery("select * from items where items.account='"+(characterID)+"' and items.slot='"+(i)+"' order BY items.slot asc");
							rsmd = rs.getMetaData();
							if(rs.next())
							for(int j = 1; j <= rsmd.getColumnCount(); j++)
							{
								Inventory += rsmd.getColumnLabel(j) + ":" + rs.getString(j) + ":";
							}
						}
						//System.out.println(Inventory);
						send(Inventory);
						sleep(20);
					}
					{
						rs = stmt.executeQuery("select Items.Image, Items.Type, Items.X, Items.Y from items where items.account='"+(1)+"' and items.map="+(database.map)+" and items.slot="+(10000)+" order BY items.y asc");
						rsmd = rs.getMetaData();
						rs.last();
						GroundItems += (rs.getRow()) + ":";
						rs.beforeFirst();



						while(rs.next())
						for(int j = 1; j <= rsmd.getColumnCount(); j++)
						{
							GroundItems += rsmd.getColumnLabel(j) + ":" + rs.getString(j) + ":";
						}
						//GroundItems += +rs.getString("items.image") + ":" + rs.getString("items.x") + ":" + rs.getString("items.y") + ":";
						//System.out.println(GroundItems);
						send(GroundItems);
						sleep(20);
					}
					{
						for(int i = 0; i <= 47; i++)
						{
							rs = stmt.executeQuery("select * from items where items.account='"+(characterID)+"' and items.slot='"+(i)+"' order BY items.slot asc");
							rsmd = rs.getMetaData();
							if(rs.next())
							for(int j = 1; j <= rsmd.getColumnCount(); j++)
							{
								Equipment += rsmd.getColumnLabel(j) + ":" + rs.getString(j) + ":";
							}
						}
						//System.out.println(Equipment);
						send(Equipment);
						sleep(20);
					}
				}
				catch(Exception e)
				{
					if(Global.debug)
					e.printStackTrace();
				}
			}
		}
	}
	class CharacterMover
	{
		Statement stmt;
		ResultSet rs;
		ResultSetMetaData rsmd;
		String name;
		int map, x, y, frame;
		boolean flip;

		CharacterMover(String name)
		{
			this.name = name;
			try
			{
				stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
				rs = stmt.executeQuery("select * from characters where characters.server='"+Global.server+"' and characters.name='"+name+"'");
				rsmd = rs.getMetaData();
				rs.next();
				map = rs.getInt("characters.Map");
				x = rs.getInt("characters.Y");
				y = rs.getInt("characters.X");
				frame = rs.getInt("characters.Frame");
				flip = rs.getBoolean("characters.Flip");
			}
			catch(Exception e)
			{
				if(Global.debug)
				e.printStackTrace();
			}
		}

		 public void MoveMapXY(String[] direction)//synchronized
		{
			map = Integer.parseInt(direction[1]);
			x = Integer.parseInt(direction[2]);
			y = Integer.parseInt(direction[3]);
			frame = Integer.parseInt(direction[4]);
			flip = Boolean.parseBoolean(direction[5]);
			try
			{
				rs.updateInt("characters.map",map);
				rs.updateInt("characters.x",x);
				rs.updateInt("characters.y",y);
				rs.updateInt("characters.frame",frame);
				rs.updateBoolean("characters.flip",flip);
				rs.updateString("Characters.Channel",Global.channel);
				rs.updateBoolean("Characters.LoggedIn",true);
				rs.updateRow();
				//send("Location:" + rs.getString("characters.map") + ":" + rs.getString("characters.x") + ":" + rs.getString("characters.y") + ":");
			}
			catch(Exception e)
			{
				if(Global.debug)
				e.printStackTrace();
			}
		}
	}
}

class Global
{
	static boolean debug = true;
	static String server = "mashumafi";
	static String channel = "mashumafi1";
}