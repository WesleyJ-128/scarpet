
__on_player_swings_hand(player, hand)-> (
    if(hand=='mainhand' && player~'holds':0=='brewing_stand',
        call_gui_menu(global_Test_GUI, player)
    )
);

//Config

global_inventory_sizes={
    'generic_3x3'->9,
    'generic_9x1'->9,
    'generic_9x2'->18,
    'generic_9x3'->27,
    'generic_9x4'->36,
    'generic_9x5'->45,
    'generic_9x6'->54
};

//Certain names are subject to change, so instead I'll store them in global variables while I'm still fiddling with exact nomenclature
global_static_buttons='buttons';
global_dynamic_buttons='dynamic_buttons';
global_storage_slots='storage_slots';

global_Test={
    'inventory_shape'->'generic_3x3',
    'title'->format('db Test GUI menu!'),
    global_static_buttons->{
        0->['red_stained_glass_pane', _(button)->print('Pressed the red button!')],
        4->['green_stained_glass_pane', _(button)->print(str('Clicked with %s button', if(button, 'Right', 'Left')))]
    },
    global_dynamic_buttons->{
        1->{ //Blue button to black button
            'icon'->'blue_stained_glass_pane',
            'action'->_(screen, button)->inventory_set(screen, 1, 1, if(inventory_get(screen, 1):0=='blue_stained_glass_pane', 'black_stained_glass_pane', 'blue_stained_glass_pane'));
        },
        6->{ //Turns the slot above purple
            'icon'->'lime_stained_glass_pane',
            'action'->_(screen)->(
                inventory_set(screen, button, 3, 1, if(inventory_get(screen, 3)==null, 'purple_stained_glass_pane', 'air'));
            )
        }
    },
    global_storage_slots->{ //These slots can be used for storage by the player
        8->['stone', 4, null] //This is simply the first item that will be available in the slot, it will subsequently be overwritten by whatever the player places in that slot
    }
};

new_gui_menu(gui_screen)->( //Stores GUI data in intermediary map form, so the programmer can call them at any time with call_gui_menu() function
    if(type(gui_screen)!='map' || !has(gui_screen, 'inventory_shape'),
        throw('Invalid gui creation: '+gui_screen)
    );

    inventory_shape = gui_screen:'inventory_shape';

    inventory_size = global_inventory_sizes:inventory_shape;

    if(inventory_size==0,
        throw('Invalid gui creation: Must be one of '+keys(global_inventory_sizes)+', not '+inventory_shape)
    );

    {
        'inventory_shape'->inventory_shape, //shape of the inventory, copied from above
        'title'->gui_screen:'title', //Fancy GUI title
        'on_created'->_(screen, outer(gui_screen))->(// Fiddling with the screen after it's made to add fancy visual bits
            for(gui_screen:global_static_buttons,
                inventory_set(screen, _, 1, gui_screen:global_static_buttons:_:0)
            );
            for(gui_screen:global_dynamic_buttons,
                inventory_set(screen, _, 1, gui_screen:global_dynamic_buttons:_:'icon')
            );
            for(gui_screen:global_storage_slots,
                [item, count, nbt] = gui_screen:global_storage_slots:_ || ['air', 0, null];
                inventory_set(screen, _, count, item, nbt)
            );
        ),
        'callback'->_(screen, player, action, data, outer(gui_screen), outer(inventory_size))->(//This is where most of the action happens
            slot = data:'slot'; //Grabbing slot, this is the focus of the action

            if(action=='pickup', //This is equivalent of clicking (button action)
                if(has(gui_screen:global_static_buttons, slot), //Plain, vanilla button
                    call(gui_screen:global_static_buttons:slot:1, data:'button'),
                    has(gui_screen:global_dynamic_buttons, slot), //A more exciting button
                    call(gui_screen:global_dynamic_buttons:slot:'action', screen, data:'button')
                );
            );

            //Saving items in storage slots when closing
            if(action=='close',
                for(gui_screen:global_storage_slots,
                    gui_screen:global_storage_slots:_ = inventory_get(screen, _);
                );
            );

            //Disabling quick move cos it messes up the GUI, and there's no reason to allow it
            //Also preventing the player from tampering with button slots
            //Unless the slot is marked as a storage slot, in which case we allow it
            if((action=='quick_move'||slot<inventory_size)&&!has(gui_screen:global_storage_slots,slot),
                'cancel'
            );
        )
    }
);

call_gui_menu(gui_menu, player)->( //Opens the screen to the player, returns screen for further manipulation
    screen = create_screen(player, gui_menu:'inventory_shape', gui_menu:'title', gui_menu:'callback');
    call(gui_menu:'on_created', screen);
    screen
);

global_Test_GUI = new_gui_menu(global_Test);
