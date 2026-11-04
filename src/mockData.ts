import { GigWorker, ClientTask, InterviewQuestion } from './types';

export const INITIAL_WORKERS: GigWorker[] = [
  {
    id: "w1",
    name: "Fredrick Anziko",
    email: "anzikofred@gmail.com",
    phone: "+256 772 102 938",
    location: "Kampala Central, Nakawa",
    category: "Electrical",
    skills: ["Industrial Wiring", "Solar Inverter Setup", "Breaker Panel Repair", "CCTV Installation"],
    equipments: ["Fluke Multimeter", "Insulated Toolkit", "Ladder", "Wire Crimpers"],
    experienceYears: 6,
    rating: 4.9,
    reviewCount: 38,
    completedTasksCount: 42,
    verificationStatus: "VETTED_APPROVED",
    aiVettingSummary: "Vetted & Verified: Passed Kivulu Electrical Competence AI Interview with 95% score. Verified Google Calendar availability.",
    hourlyRate: "UGX 45,000 / hr",
    bio: "Certified electrical technician specializing in home and commercial power setups, emergency repairs, and solar backup systems.",
    googleCalendarConnected: true
  },
  {
    id: "w2",
    name: "Beatrice Akello",
    email: "beatrice.a@kivulu.org",
    phone: "+256 701 445 921",
    location: "Wandegeya / Makerere",
    category: "IT & Tech",
    skills: ["Network Router Config", "PC Hardware Repair", "Smart Home IoT Setup", "Wi-Fi Mesh Installation"],
    equipments: ["Cat6 Crimping Kit", "LAN Cable Tester", "Solder Station", "Toolkit"],
    experienceYears: 4,
    rating: 4.8,
    reviewCount: 29,
    completedTasksCount: 31,
    verificationStatus: "VETTED_APPROVED",
    aiVettingSummary: "Vetted & Verified: Highly proficient in networking, troubleshooting, and smart office devices.",
    hourlyRate: "UGX 50,000 / hr",
    bio: "IT Infrastructure & Network Technician. I quickly diagnose hardware faults, set up fast office Wi-Fi, and configure security cameras.",
    googleCalendarConnected: true
  },
  {
    id: "w3",
    name: "Moses Mugisha",
    email: "moses.m@kivulu.org",
    phone: "+256 788 331 002",
    location: "Entebbe / Katabi",
    category: "Plumbing",
    skills: ["High-Pressure Leak Repair", "Water Tank Installation", "Drain Unclogging", "Water Heater Fittings"],
    equipments: ["Pipe Wrench Set", "Drain Snake Cable", "Thread Sealant", "Pressure Gauge"],
    experienceYears: 7,
    rating: 4.95,
    reviewCount: 54,
    completedTasksCount: 60,
    verificationStatus: "VETTED_APPROVED",
    aiVettingSummary: "Vetted & Verified: Master plumber with exceptional reliability score and zero reported leak returns.",
    hourlyRate: "UGX 40,000 / hr",
    bio: "Master plumber with 7 years on-site experience. Specialized in rapid emergency leak stoppage and modern bathroom fittings.",
    googleCalendarConnected: true
  },
  {
    id: "w4",
    name: "Sarah Namukasa",
    email: "sarah.n@kivulu.org",
    phone: "+256 752 900 114",
    location: "Bugolobi / Ntinda",
    category: "Cleaning",
    skills: ["Deep House Cleaning", "Post-Construction Clean-up", "Carpet Steam Wash", "Office Sanitization"],
    equipments: ["Industrial Vacuum Cleaner", "Steam Sanitizer", "Eco-Friendly Solvents"],
    experienceYears: 3,
    rating: 4.85,
    reviewCount: 22,
    completedTasksCount: 25,
    verificationStatus: "VETTED_APPROVED",
    aiVettingSummary: "Vetted & Verified: Top-rated detail-oriented hygiene and sanitization professional.",
    hourlyRate: "UGX 35,000 / hr",
    bio: "Professional deep cleaning technician. I ensure pristine, sparkling spaces for homes, moving transitions, and corporate offices.",
    googleCalendarConnected: true
  },
  {
    id: "w5",
    name: "Joseph Kintu",
    email: "joseph.k@kivulu.org",
    phone: "+256 774 219 883",
    location: "Kisasi / Kyebando",
    category: "Carpentry",
    skills: ["Custom Cabinetry", "Door Lock & Frame Repair", "Furniture Assembly", "Roof Truss Repair"],
    equipments: ["Circular Saw", "Cordless Drill Set", "Wood Router", "Clamps"],
    experienceYears: 5,
    rating: 4.75,
    reviewCount: 18,
    completedTasksCount: 20,
    verificationStatus: "VETTED_APPROVED",
    aiVettingSummary: "Vetted & Verified: Passed timber work precision assessment and custom furniture fitting.",
    hourlyRate: "UGX 42,000 / hr",
    bio: "Skilled woodworker and carpenter. Passionate about durable custom furniture, precise door alignment, and wood repairs.",
    googleCalendarConnected: true
  }
];

export const INITIAL_TASKS: ClientTask[] = [
  {
    id: "t101",
    clientName: "Dr. David Kato",
    clientEmail: "david.kato@healthcare.ug",
    clientPhone: "+256 701 992 334",
    title: "Emergency Circuit Breaker Tripping Repair",
    description: "Our clinic's main electrical sub-panel trips whenever the air conditioner and medical sterilizer run simultaneously. Need a vetted electrician to inspect load distribution and replace faulty breaker.",
    category: "Electrical",
    location: "Nakawa Medical Center, Kampala",
    deadline: "Urgent (Today)",
    budget: "UGX 120,000",
    status: "OPEN",
    postedAt: "2 hours ago"
  },
  {
    id: "t102",
    clientName: "Grace Tumusiime",
    clientEmail: "grace.t@designstudio.co.ug",
    clientPhone: "+256 782 554 110",
    title: "Office High-Speed Wi-Fi Router & Mesh Setup",
    description: "Configuring 3 mesh access points in our new 2-floor design studio for seamless roaming and secure guest Wi-Fi network isolation.",
    category: "IT & Tech",
    location: "Bugolobi Expressway, Kampala",
    deadline: "Tomorrow 10:00 AM",
    budget: "UGX 150,000",
    status: "MATCHED",
    assignedWorkerId: "w2",
    assignedWorkerName: "Beatrice Akello",
    scheduledTime: "2025-11-05T10:00:00",
    calendarEventLink: "https://calendar.google.com/calendar/r",
    postedAt: "Yesterday"
  },
  {
    id: "t103",
    clientName: "Samuel Ochieng",
    clientEmail: "sochieng@gmail.com",
    clientPhone: "+256 773 881 229",
    title: "Overhead Water Tank Float Valve Replacement",
    description: "The 2,000-liter overhead PVC water tank is overflowing automatically. Need a plumber to install a heavy-duty copper float valve and inspect supply line joints.",
    category: "Plumbing",
    location: "Kyanja Ring Road, Kampala",
    deadline: "In 2 days",
    budget: "UGX 90,000",
    status: "OPEN",
    postedAt: "3 hours ago"
  }
];

export const VETTING_QUESTIONS: Record<string, InterviewQuestion[]> = {
  Electrical: [
    {
      id: 1,
      questionText: "Before troubleshooting a tripping main breaker in a commercial panel, what safety step is required first?",
      options: [
        "Replace breaker immediately",
        "Isolate power, lock out/tag out, and verify voltage with a calibrated multimeter",
        "Increase breaker amperage rating",
        "Bypass the surge protector"
      ],
      correctOptionIndex: 1,
      explanation: "Always de-energize and verify zero voltage prior to physical inspection."
    },
    {
      id: 2,
      questionText: "What wire color is standard for equipment grounding in standard AC single-phase installations?",
      options: ["Red or Brown", "Black", "Green or Bare Copper", "Blue"],
      correctOptionIndex: 2,
      explanation: "Green or bare copper wire is universally reserved for protective earth grounding."
    },
    {
      id: 3,
      questionText: "What tool is essential to measure insulation resistance in electrical motors and cables?",
      options: ["Clamp Meter", "Megohmmeter (Megger)", "Oscilloscope", "Wire Stripper"],
      correctOptionIndex: 1,
      explanation: "A Megohmmeter tests high-voltage insulation integrity."
    }
  ],
  Plumbing: [
    {
      id: 1,
      questionText: "How do you treat a high-pressure pipe leak prior to replacing a damaged PVC section?",
      options: [
        "Shut off the main supply stop valve, drain residual line water, and dry the joint area",
        "Wrap tightly with duct tape while pressurized",
        "Pour cement over the wet pipe",
        "Heat the pipe with a propane torch while running"
      ],
      correctOptionIndex: 0,
      explanation: "Isolation and pressure release are vital before pipe cutting and solvent bonding."
    },
    {
      id: 2,
      questionText: "What sealant is applied to threaded metal pipe fittings to ensure a watertight seal?",
      options: ["Hot Glue", "PTFE / Teflon Thread Tape or Pipe Dope", "Silicone Caulk", "Epoxy resin"],
      correctOptionIndex: 1,
      explanation: "PTFE thread sealant prevents fluid seepage across male/female threads."
    }
  ]
};
