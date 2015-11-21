package com.snowgears.machines.gravity;


import com.snowgears.machines.Machine;
import com.snowgears.machines.MachineType;
import org.bukkit.Location;

import java.util.UUID;

public class GravityMachine extends Machine {

    //TODO will have to play around with super call/method in order to be able to use Machine.create() and whatnot throughout program
    public GravityMachine(MachineType type, UUID owner, Location baseLocation){
        super(type, owner, baseLocation);
    }

}
