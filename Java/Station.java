class Station {

	public static void main(String args[]) {
		Server station = new Server(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]));
		for (int i = 3; i < args.length; i++) {
			station.addAdjancent(Integer.parseInt(args[i]));
		}
		System.out.println(station.toString());
	}
}
