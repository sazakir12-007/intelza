package com.ht.intelza.data

import com.ht.intelza.domain.AnswerOption

/**
 * Test classes and question banks for trying the app: Grade 2 and Grade 3 with ten
 * students each, and for each grade two topics of ten multiple-choice questions in every
 * default subject. Added and removed from Settings → Sample data.
 */
object SampleData {

    data class SampleClass(val name: String, val students: List<String>)

    data class SampleQuestion(val text: String, val options: List<String>, val correct: AnswerOption)

    data class SampleTopic(
        val subject: String,
        val grade: String,
        val name: String,
        val questions: List<SampleQuestion>,
    )

    val classes = listOf(
        SampleClass(
            "Grade 2 – Test",
            listOf(
                "Aarav Sharma", "Diya Patel", "Ishaan Reddy", "Ananya Iyer", "Vihaan Gupta",
                "Saanvi Nair", "Arjun Mehta", "Myra Khan", "Kabir Singh", "Aadhya Das",
            ),
        ),
        SampleClass(
            "Grade 3 – Test",
            listOf(
                "Reyansh Joshi", "Kiara Menon", "Atharv Kulkarni", "Anika Bose", "Vivaan Rao",
                "Prisha Shah", "Ayaan Ali", "Navya Pillai", "Dhruv Verma", "Ira Banerjee",
            ),
        ),
    )

    val topics: List<SampleTopic> = listOf(
        // Maths
        topic(
            "Maths", "2", "Numbers up to 100",
            q("Which number comes just after 49?", "48", "50", "59", "40", 'B'),
            q("Which is the biggest number?", "76", "67", "70", "61", 'A'),
            q("How many tens are there in 58?", "8", "50", "5", "13", 'C'),
            q("What is 3 tens and 4 ones?", "43", "7", "304", "34", 'D'),
            q("Which number is even?", "15", "22", "37", "41", 'B'),
            q("Which number comes just before 70?", "69", "71", "60", "80", 'A'),
            q("Count in tens: 20, 30, 40, …", "41", "45", "50", "60", 'C'),
            q("Which number is smaller than 25?", "52", "30", "25", "19", 'D'),
            q("How do we write \"sixty-two\" in numbers?", "62", "26", "602", "612", 'A'),
            q("Which number is between 88 and 90?", "87", "91", "89", "98", 'C'),
        ),
        topic(
            "Maths", "2", "Addition and Subtraction",
            q("25 + 10 = ?", "15", "35", "45", "26", 'B'),
            q("40 − 15 = ?", "25", "35", "55", "20", 'A'),
            q("18 + 7 = ?", "24", "26", "25", "11", 'C'),
            q("50 − 20 = ?", "70", "20", "25", "30", 'D'),
            q("Riya has 12 pencils and gets 8 more. How many pencils does she have now?", "4", "20", "18", "21", 'B'),
            q("9 + 9 = ?", "18", "19", "16", "0", 'A'),
            q("33 − 3 = ?", "36", "3", "30", "31", 'C'),
            q("Which pair adds up to 10?", "4 and 5", "3 and 8", "2 and 9", "6 and 4", 'D'),
            q("A bus has 30 children. 10 get off. How many are left?", "20", "40", "10", "30", 'A'),
            q("46 + 23 = ?", "23", "79", "69", "96", 'C'),
        ),
        topic(
            "Maths", "3", "Multiplication",
            q("3 × 4 = ?", "7", "34", "1", "12", 'D'),
            q("5 × 2 = ?", "10", "7", "52", "3", 'A'),
            q("6 × 3 = ?", "9", "63", "18", "16", 'C'),
            q("4 groups of 5 apples make how many apples?", "9", "20", "45", "25", 'B'),
            q("2 × 8 = ?", "16", "10", "28", "14", 'A'),
            q("7 × 1 = ?", "8", "1", "0", "7", 'D'),
            q("9 × 0 = ?", "9", "0", "90", "1", 'B'),
            q("10 × 6 = ?", "16", "106", "60", "66", 'C'),
            q("Which is the same as 3 + 3 + 3 + 3?", "4 × 3", "3 + 4", "3 × 3", "33", 'A'),
            q("5 × 5 = ?", "10", "55", "20", "25", 'D'),
        ),
        topic(
            "Maths", "3", "Shapes and Measurement",
            q("How many sides does a triangle have?", "2", "4", "3", "5", 'C'),
            q("Which shape has 4 equal sides?", "Circle", "Square", "Triangle", "Oval", 'B'),
            q("How many corners does a rectangle have?", "3", "5", "0", "4", 'D'),
            q("Which shape has no corners?", "Circle", "Square", "Triangle", "Rectangle", 'A'),
            q("How many centimetres make 1 metre?", "10", "1000", "100", "12", 'C'),
            q("Which unit do we use to measure how heavy something is?", "Kilogram", "Metre", "Litre", "Hour", 'A'),
            q("How many minutes are there in 1 hour?", "100", "24", "12", "60", 'D'),
            q("Which is the longest?", "1 cm", "1 m", "10 cm", "50 cm", 'B'),
            q("A ball is shaped like a…", "cube", "sphere", "cone", "cylinder", 'B'),
            q("How many days are there in a week?", "5", "10", "7", "30", 'C'),
        ),

        // English
        topic(
            "English", "2", "Naming and Action Words",
            q("Which word is a naming word (noun)?", "Dog", "Run", "Happy", "Quickly", 'A'),
            q("Which word is an action word (verb)?", "Table", "Blue", "Jump", "Soft", 'C'),
            q("Find the naming word: \"The cat sleeps on the mat.\"", "sleeps", "cat", "on", "the", 'B'),
            q("Which word is the name of a place?", "Pencil", "Sing", "Tall", "Delhi", 'D'),
            q("Find the action word: \"Birds fly in the sky.\"", "Birds", "fly", "sky", "the", 'B'),
            q("Which word names an animal?", "Eat", "Green", "Lion", "Under", 'C'),
            q("Which word tells what you do with a book?", "Read", "Chair", "Red", "Big", 'A'),
            q("Which word is a special name (proper noun)?", "city", "girl", "river", "Ganga", 'D'),
            q("Fill in the blank: \"The baby ___ milk.\"", "chair", "happy", "drinks", "blue", 'C'),
            q("Which word is NOT an action word?", "Swim", "Apple", "Write", "Clap", 'B'),
        ),
        topic(
            "English", "2", "Opposites and Rhyming Words",
            q("What is the opposite of \"hot\"?", "warm", "cold", "sunny", "wet", 'B'),
            q("What is the opposite of \"big\"?", "tall", "huge", "long", "small", 'D'),
            q("Which word rhymes with \"cat\"?", "hat", "cup", "dog", "sun", 'A'),
            q("What is the opposite of \"day\"?", "noon", "sun", "night", "morning", 'C'),
            q("Which word rhymes with \"ball\"?", "bell", "bowl", "bill", "tall", 'D'),
            q("What is the opposite of \"happy\"?", "glad", "sad", "funny", "kind", 'B'),
            q("Which word rhymes with \"tree\"?", "bee", "leaf", "branch", "green", 'A'),
            q("What is the opposite of \"open\"?", "push", "door", "close", "wide", 'C'),
            q("Which word rhymes with \"star\"?", "sky", "moon", "light", "car", 'D'),
            q("What is the opposite of \"up\"?", "down", "top", "over", "high", 'A'),
        ),
        topic(
            "English", "3", "Grammar Basics",
            q("Which word describes the balloon in \"a red balloon\"?", "a", "balloon", "the", "red", 'D'),
            q("Choose the right word: \"___ apple a day keeps the doctor away.\"", "An", "A", "The", "Some", 'A'),
            q("Which word can take the place of \"Sita\"?", "He", "It", "She", "They", 'C'),
            q("What is the past tense of \"go\"?", "goed", "went", "gone", "going", 'B'),
            q("Which sentence is correct?", "They are playing.", "They is playing.", "They am playing.", "They be playing.", 'A'),
            q("What is the plural of \"child\"?", "childs", "childes", "child", "children", 'D'),
            q("Fill in the blank: \"I ___ my homework yesterday.\"", "do", "did", "doing", "does", 'B'),
            q("Which word is an adjective (describing word)?", "quickly", "sing", "beautiful", "table", 'C'),
            q("Fill in the joining word: \"I like tea ___ coffee.\"", "and", "the", "is", "very", 'A'),
            q("Which sentence needs a question mark?", "I like mangoes", "Close the door", "She is my friend", "Where do you live", 'D'),
        ),
        topic(
            "English", "3", "Vocabulary and Spelling",
            q("Which spelling is correct?", "freind", "frend", "friend", "freand", 'C'),
            q("Which word means the same as \"big\"?", "tiny", "large", "slow", "thin", 'B'),
            q("Which spelling is correct?", "beutiful", "beautyful", "butiful", "beautiful", 'D'),
            q("A baby cat is called a…", "kitten", "puppy", "calf", "cub", 'A'),
            q("Which word means \"very happy\"?", "angry", "tired", "joyful", "sleepy", 'C'),
            q("A person who teaches is a…", "teacher", "farmer", "driver", "baker", 'A'),
            q("Which spelling is correct?", "skool", "shcool", "scool", "school", 'D'),
            q("Which word is the opposite of \"begin\"?", "start", "finish", "open", "try", 'B'),
            q("Where do we go to borrow books to read?", "Kitchen", "Library", "Garden", "Market", 'B'),
            q("Which word comes first in a dictionary?", "dog", "elephant", "apple", "cat", 'C'),
        ),

        // Science
        topic(
            "Science", "2", "Plants Around Us",
            q("Which part of a plant takes in water from the soil?", "Roots", "Flower", "Fruit", "Leaf", 'A'),
            q("Plants make their food in their…", "roots", "seeds", "leaves", "flowers", 'C'),
            q("What do plants need to grow?", "Toys", "Water and sunlight", "Paint", "Only sand", 'B'),
            q("Which part of a plant becomes a fruit?", "Root", "Stem", "Leaf", "Flower", 'D'),
            q("Which of these is a tree?", "Grass", "Mango", "Money plant", "Tomato", 'B'),
            q("Which plant grows in water?", "Cactus", "Neem", "Lotus", "Rose", 'C'),
            q("A climber needs ___ to grow upwards.", "support", "salt", "darkness", "wind", 'A'),
            q("Which part holds the plant up?", "Flower", "Fruit", "Seed", "Stem", 'D'),
            q("Which of these do we eat as a root?", "Spinach", "Apple", "Carrot", "Cabbage", 'C'),
            q("What does a seed grow into?", "A stone", "A new plant", "A cloud", "A toy", 'B'),
        ),
        topic(
            "Science", "2", "Animals and Their Homes",
            q("Where does a bird live?", "Burrow", "Nest", "Kennel", "Stable", 'B'),
            q("Where does a pet dog live?", "Hive", "Den", "Web", "Kennel", 'D'),
            q("Which animal lives in water?", "Fish", "Cow", "Cat", "Goat", 'A'),
            q("Which animal gives us milk?", "Hen", "Frog", "Cow", "Snake", 'C'),
            q("Bees live in a…", "nest", "den", "burrow", "hive", 'D'),
            q("Which animal can fly?", "Elephant", "Parrot", "Lion", "Horse", 'B'),
            q("A frog can live on land and in…", "water", "trees", "the sky", "a hive", 'A'),
            q("Which animal lives in a burrow?", "Eagle", "Monkey", "Rabbit", "Whale", 'C'),
            q("A spider makes a…", "shell", "nest", "hive", "web", 'D'),
            q("Which animal is a pet?", "Cat", "Tiger", "Bear", "Crocodile", 'A'),
        ),
        topic(
            "Science", "3", "Our Body and Food",
            q("Which body part do we use to see?", "Ears", "Nose", "Tongue", "Eyes", 'D'),
            q("The heart pumps…", "blood", "air", "food", "water", 'A'),
            q("Which food gives us protein?", "Sugar", "Butter", "Dal (pulses)", "Candy", 'C'),
            q("Which part of the body helps us breathe?", "Stomach", "Lungs", "Heart", "Brain", 'B'),
            q("When should we wash our hands?", "Before eating", "Once a week", "Never", "Only after sleeping", 'A'),
            q("Which of these is junk food?", "Banana", "Carrot", "Milk", "Chips", 'D'),
            q("Which food helps keep our teeth and bones strong?", "Cola", "Milk", "Chocolate", "Chips", 'B'),
            q("Fruits and vegetables give us…", "plastic", "smoke", "vitamins and minerals", "dust", 'C'),
            q("Which bone protects the brain?", "Skull", "Ribs", "Knee", "Toe", 'A'),
            q("How many times a day should we brush our teeth?", "Never", "Once a month", "Five times", "Twice", 'D'),
        ),
        topic(
            "Science", "3", "Water, Air and Weather",
            q("Water turns into ice when it is…", "heated", "stirred", "frozen", "poured", 'C'),
            q("What do we call water falling from clouds?", "Snow only", "Rain", "Fog", "Dew", 'B'),
            q("Which gas do we need to breathe?", "Smoke", "Carbon dioxide", "Steam", "Oxygen", 'D'),
            q("When water is boiled, it turns into…", "steam", "ice", "sand", "salt", 'A'),
            q("Which is the hottest season in India?", "Winter", "Monsoon", "Summer", "Spring", 'C'),
            q("Moving air is called…", "wind", "rain", "cloud", "smoke", 'A'),
            q("Why should we not waste water?", "It is a toy", "It is free", "It is heavy", "It is precious", 'D'),
            q("Which of these needs air to burn?", "Ice", "A candle flame", "A stone", "Water", 'B'),
            q("A rainbow can appear when the sun shines during…", "the night", "rain", "a dark storm", "an eclipse", 'B'),
            q("What keeps us dry in the rain?", "Fan", "Sunglasses", "Umbrella", "Torch", 'C'),
        ),

        // Social Studies
        topic(
            "Social Studies", "2", "My Family and Neighbourhood",
            q("Your father's mother is your…", "grandmother", "aunt", "sister", "cousin", 'A'),
            q("Who helps us when we are sick?", "Postman", "Carpenter", "Doctor", "Tailor", 'C'),
            q("Who delivers letters?", "Farmer", "Postman", "Cobbler", "Barber", 'B'),
            q("Where do we go to buy vegetables?", "School", "Hospital", "Police station", "Market", 'D'),
            q("Who makes furniture from wood?", "Potter", "Carpenter", "Dentist", "Pilot", 'B'),
            q("Who keeps our streets safe?", "Baker", "Painter", "Police officer", "Singer", 'C'),
            q("A family where grandparents, parents and children live together is a…", "joint family", "team", "school", "club", 'A'),
            q("Where can we borrow books?", "Bank", "Bakery", "Petrol pump", "Library", 'D'),
            q("Who grows crops for our food?", "Pilot", "Tailor", "Farmer", "Driver", 'C'),
            q("Who puts out fires?", "Teacher", "Firefighter", "Shopkeeper", "Cook", 'B'),
        ),
        topic(
            "Social Studies", "2", "Transport and Communication",
            q("Which of these travels on water?", "Bus", "Ship", "Train", "Car", 'B'),
            q("Which of these flies in the sky?", "Boat", "Truck", "Cycle", "Aeroplane", 'D'),
            q("Which vehicle runs on rails?", "Train", "Bus", "Scooter", "Ship", 'A'),
            q("What do we use to talk to someone far away?", "Spoon", "Pillow", "Mobile phone", "Brush", 'C'),
            q("Which vehicle has two wheels and is moved by pedalling?", "Car", "Bus", "Train", "Bicycle", 'D'),
            q("At a traffic light, red means…", "go", "stop", "run", "turn", 'B'),
            q("Where should we cross the road?", "Zebra crossing", "Middle of the road", "Behind a bus", "Anywhere", 'A'),
            q("Where do we send letters and parcels from?", "Ambulance", "Fire station", "Post office", "Bus stop", 'C'),
            q("Which vehicle takes sick people to hospital?", "Taxi", "Tractor", "Truck", "Ambulance", 'D'),
            q("At a traffic light, green means…", "go", "stop", "wait", "turn back", 'A'),
        ),
        topic(
            "Social Studies", "3", "Our Country India",
            q("What is the capital of India?", "Mumbai", "Kolkata", "Chennai", "New Delhi", 'D'),
            q("What is the national animal of India?", "Tiger", "Lion", "Elephant", "Peacock", 'A'),
            q("What is the national bird of India?", "Parrot", "Crow", "Peacock", "Pigeon", 'C'),
            q("Which colour is at the top of the Indian flag?", "Green", "Saffron", "White", "Blue", 'B'),
            q("Independence Day is celebrated on…", "15 August", "26 January", "2 October", "14 November", 'A'),
            q("What is the national flower of India?", "Rose", "Sunflower", "Jasmine", "Lotus", 'D'),
            q("Who wrote the national anthem \"Jana Gana Mana\"?", "Mahatma Gandhi", "Rabindranath Tagore", "Jawaharlal Nehru", "Subhas Chandra Bose", 'B'),
            q("Republic Day is celebrated on…", "15 August", "5 September", "26 January", "1 May", 'C'),
            q("How many spokes does the Ashoka Chakra on our flag have?", "24", "12", "10", "30", 'A'),
            q("What is the national fruit of India?", "Apple", "Banana", "Orange", "Mango", 'D'),
        ),
        topic(
            "Social Studies", "3", "Maps and Directions",
            q("The sun rises in the…", "West", "North", "East", "South", 'C'),
            q("The sun sets in the…", "East", "West", "North", "South", 'B'),
            q("What does a map show?", "Only the sky", "Music", "People's names", "Places seen from above", 'D'),
            q("A small sign on a map that stands for something is called a…", "symbol", "letter", "number", "stamp", 'A'),
            q("If you face the rising sun, which direction is behind you?", "North", "East", "West", "South", 'C'),
            q("How many main directions are there?", "Four", "Two", "Six", "Eight", 'A'),
            q("Which of these is a model of the Earth?", "Clock", "Calendar", "Book", "Globe", 'D'),
            q("On most maps, the top of the map is…", "South", "North", "East", "West", 'B'),
            q("Blue on a map usually shows…", "mountains", "water", "roads", "forests", 'B'),
            q("Which tool helps us find directions?", "Thermometer", "Ruler", "Compass", "Calculator", 'C'),
        ),
    )

    private fun topic(subject: String, grade: String, name: String, vararg questions: SampleQuestion) =
        SampleTopic(subject, grade, name, questions.toList())

    private fun q(text: String, a: String, b: String, c: String, d: String, answer: Char) =
        SampleQuestion(text, listOf(a, b, c, d), AnswerOption.valueOf(answer.toString()))
}
