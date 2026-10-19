/* ORS Hindi UI translations for existing JSP text.
 * The selected language is resolved by LanguageFilter.
 * This file translates visible UI text only.
 */
(function () {
    'use strict';

    if (!document.documentElement ||
        document.documentElement.lang !== 'hi') {
        return;
    }

    const translations = {
        'English': 'अंग्रेज़ी',
        'Hindi': 'हिन्दी',
        'IN Hindi': 'हिन्दी',
        'Home': 'होम',
        'Login': 'लॉगिन',
        'Logout': 'लॉगआउट',
        'Guest': 'अतिथि',

        'Marksheet': 'अंकपत्र',
        'Query': 'खोज',
        'Get Marksheet': 'अंकपत्र देखें',
        'Merit List & Benchmarks': 'मेरिट सूची और तुलना',
        'Subject Grading': 'विषय के अंक',
        'Add / Grade Marksheet': 'अंकपत्र जोड़ें / अंक दें',
        'Marksheet List': 'अंकपत्र सूची',
        'Department Audit': 'विभागीय ऑडिट',
        'Course Roster': 'कोर्स के विद्यार्थी',

        'Academics': 'शैक्षणिक प्रबंधन',
        'Colleges': 'कॉलेज',
        'Add College': 'कॉलेज जोड़ें',
        'College List': 'कॉलेज सूची',
        'Courses': 'कोर्स',
        'Curriculum & Courses': 'पाठ्यक्रम और कोर्स',
        'Add Course': 'कोर्स जोड़ें',
        'Course List': 'कोर्स सूची',
        'Subjects': 'विषय',
        'Add Subject': 'विषय जोड़ें',
        'Subject List': 'विषय सूची',

        'Enrollment': 'नामांकन',
        'Add Enrollment': 'नामांकन जोड़ें',
        'Enrollment List': 'नामांकन सूची',
        'Faculty': 'शिक्षक',
        'Academic Staff': 'शैक्षणिक कर्मचारी',
        'Add Faculty': 'शिक्षक जोड़ें',
        'Faculty List': 'शिक्षक सूची',
        'Add Allocation': 'आवंटन जोड़ें',
        'Users': 'उपयोगकर्ता',
        'Add User': 'उपयोगकर्ता जोड़ें',
        'User List': 'उपयोगकर्ता सूची',
        'Roles': 'भूमिकाएँ',
        'Add Role': 'भूमिका जोड़ें',
        'Role List': 'भूमिका सूची',
        'Reports': 'रिपोर्ट',

        'My Profile': 'मेरी प्रोफ़ाइल',
        'Change Password': 'पासवर्ड बदलें',
        'Update Password': 'पासवर्ड अपडेट करें',
        'Save': 'सहेजें',
        'Delete': 'हटाएँ',
        'Cancel': 'रद्द करें',
        'Back': 'वापस',
        'Search': 'खोजें',
        'Edit': 'संपादित करें',
        'Update': 'अपडेट करें',
        'Submit': 'जमा करें',

        'Welcome': 'स्वागत है',
        'Welcome to ORS': 'ORS में आपका स्वागत है',
        'Online Result System': 'ऑनलाइन परिणाम प्रणाली',
        'Online Result System — Course-Wise Result & Departmental Management Portal':
            'ऑनलाइन परिणाम प्रणाली — कोर्स-वार परिणाम और विभागीय प्रबंधन पोर्टल',
        'Course-Wise Result & Departmental Management Portal':
            'कोर्स-वार परिणाम और विभागीय प्रबंधन पोर्टल',
        'Student Result & Benchmark Portal':
            'विद्यार्थी परिणाम और तुलना पोर्टल',

        'Administration Panel': 'प्रशासन पैनल',
        'Program Chair / HOD Oversight Panel':
            'प्रोग्राम चेयर / विभागाध्यक्ष पैनल',
        'Faculty Academic Performance & Grading Panel':
            'शिक्षक प्रदर्शन और अंक निर्धारण पैनल',
        'Get Started': 'शुरू करें',

        'Course-Wise Marksheet': 'कोर्स-वार अंकपत्र',
        'View course & subject results and class benchmarks':
            'कोर्स और विषय के परिणाम तथा कक्षा की तुलना देखें',
        'Peer Benchmarks': 'सहपाठी तुलना',
        'Compare against anonymized class rankings':
            'गोपनीय कक्षा रैंकिंग से तुलना करें',
        'View and edit your details':
            'अपनी जानकारी देखें और संपादित करें',
        'Update your 8-character password':
            'अपना 8-अक्षर का पासवर्ड अपडेट करें',
        'Enrollment metrics, staff loads & curriculum':
            'नामांकन आँकड़े, कर्मचारी कार्यभार और पाठ्यक्रम',
        'Supervise educator profiles & teaching loads':
            'शिक्षक प्रोफ़ाइल और शिक्षण कार्यभार देखें',
        'Inspect active programs and modules':
            'सक्रिय कार्यक्रम और मॉड्यूल देखें',
        'Enrollment Roster': 'नामांकन सूची',
        'Monitor student registrations': 'विद्यार्थी पंजीकरण देखें',
        'View students enrolled in your course':
            'अपने कोर्स में नामांकित विद्यार्थी देखें',
        'Enter Subject Marks': 'विषय के अंक दर्ज करें',
        'Log course-wise student subject scores':
            'कोर्स-वार विषय अंक दर्ज करें',
        'Manage Marksheets': 'अंकपत्र प्रबंधित करें',
        'Review and update subject grades':
            'विषय के अंक देखें और अपडेट करें',
        'Merit List': 'मेरिट सूची',
        'View course merit rankings': 'कोर्स की मेरिट रैंकिंग देखें',

        'Profile': 'प्रोफ़ाइल',
        'Login ID (Email)': 'लॉगिन आईडी (ईमेल)',
        'Login ID': 'लॉगिन आईडी',
        'Email Id (Login)': 'ईमेल आईडी (लॉगिन)',
        'Email ID': 'ईमेल आईडी',
        'Email': 'ईमेल',
        'First Name': 'पहला नाम',
        'Last Name': 'अंतिम नाम',
        'Name': 'नाम',
        'Gender': 'लिंग',
        'Male': 'पुरुष',
        'Female': 'महिला',
        'Mobile No': 'मोबाइल नंबर',
        'Mobile Number': 'मोबाइल नंबर',
        'Date of Birth (mm/dd/yyyy)': 'जन्म तिथि (महीना/दिन/वर्ष)',
        'Date of Birth': 'जन्म तिथि',

        'Old Password (8 Characters)': 'पुराना पासवर्ड (8 अक्षर)',
        'New Password (8 Characters)': 'नया पासवर्ड (8 अक्षर)',
        'Confirm Password (8 Characters)': 'पासवर्ड की पुष्टि करें (8 अक्षर)',
        'Password': 'पासवर्ड',
        'Old Password': 'पुराना पासवर्ड',
        'New Password': 'नया पासवर्ड',
        'Confirm Password': 'पासवर्ड की पुष्टि करें',

        'Course Name': 'कोर्स का नाम',
        'Subject Name': 'विषय का नाम',
        'College Name': 'कॉलेज का नाम',
        'Course': 'कोर्स',
        'Subject': 'विषय',
        'Student': 'विद्यार्थी',
        'Student Name': 'विद्यार्थी का नाम',
        'Faculty Name': 'शिक्षक का नाम',
        'Roll No': 'रोल नंबर',
        'Roll Number': 'रोल नंबर',
        'Marks': 'अंक',
        'Total': 'कुल',
        'Percentage': 'प्रतिशत',
        'Result': 'परिणाम',
        'Rank': 'रैंक',
        'Status': 'स्थिति',
        'Action': 'कार्रवाई',
        'Address': 'पता',
        'Description': 'विवरण',

        'Enter your email ID': 'अपना ईमेल आईडी दर्ज करें',
        'Enter 8-character password': '8-अक्षर का पासवर्ड दर्ज करें',
        'Enter 8-character old password':
            'पुराना 8-अक्षर का पासवर्ड दर्ज करें',
        'Enter 8-character new password':
            'नया 8-अक्षर का पासवर्ड दर्ज करें',
        'Confirm 8-character password':
            '8-अक्षर के पासवर्ड की पुष्टि करें',
        'Only letters are allowed': 'केवल अक्षर अनुमत हैं',
        '10 digits starting with 6, 7, 8, or 9':
            '10 अंक दर्ज करें; पहला अंक 6, 7, 8 या 9 होना चाहिए',

        'Forgot my password': 'पासवर्ड भूल गए',
        'Forgot / Reset Your Password': 'पासवर्ड भूल गए / रीसेट करें',
        'Request a single-use, 15-minute reset token by email, then use it below to set a new password.':
            'ईमेल से एक बार उपयोग होने वाला, 15 मिनट तक मान्य रीसेट टोकन मँगाएँ और नीचे नया पासवर्ड सेट करें।',
        'Request reset token': 'रीसेट टोकन मँगाएँ',
        'Send reset instructions': 'रीसेट निर्देश भेजें',
        'Redeem Single-Use Reset Token':
            'एक बार उपयोग होने वाला रीसेट टोकन दर्ज करें',
        'Reset token': 'रीसेट टोकन',
        'New password': 'नया पासवर्ड',
        'Confirm password': 'पासवर्ड की पुष्टि करें',
        'ResetPassword': 'पासवर्ड रीसेट करें',
        'Go': 'आगे बढ़ें',
        'Enter your registered email address':
            'अपना पंजीकृत ईमेल पता दर्ज करें',
        'Paste the 64-character token': '64-अक्षरों वाला टोकन चिपकाएँ',
        'Paste 64-character token': '64-अक्षरों वाला टोकन चिपकाएँ',
        'Enter exactly 8 characters': 'ठीक 8 अक्षर दर्ज करें',
        'Re-enter the new password': 'नया पासवर्ड दोबारा दर्ज करें',
        'The password must contain exactly 8 characters.':
            'पासवर्ड में ठीक 8 अक्षर होने चाहिए।',
        'For account security, the same confirmation is shown whether or not the address is registered.':
            'खाते की सुरक्षा के लिए, ईमेल पते के पंजीकृत होने या न होने पर एक जैसा संदेश दिखाया जाता है।',
        'If an account is registered with that email address, a single-use password reset token has been sent.':
            'यदि इस ईमेल पते से खाता पंजीकृत है, तो एक बार उपयोग होने वाला पासवर्ड रीसेट टोकन भेज दिया गया है।',
        'Unable to generate password reset token':
            'पासवर्ड रीसेट टोकन नहीं बनाया जा सका',
        'Invalid or expired password reset token.':
            'रीसेट टोकन अमान्य है या उसकी समय-सीमा समाप्त हो गई है।',
        'Your password has been reset. Please log in with your new 8-character password':
            'पासवर्ड रीसेट हो गया है। नए 8-अक्षर के पासवर्ड से लॉगिन करें',

        'Authorization Alert:': 'अनुमति संबंधी चेतावनी:',
        'All rights reserved.': 'सर्वाधिकार सुरक्षित।',
        'All Courses': 'सभी कोर्स',
        'Select': 'चुनें',
        'Select Course': 'कोर्स चुनें',
        'Select Subject': 'विषय चुनें',
        '8 Characters': '8 अक्षर',
        'Above Class Avg': 'कक्षा औसत से अधिक',
        'Below Class Avg': 'कक्षा औसत से कम',
        'Active Enrollments': 'सक्रिय नामांकन',
        'Active Programs': 'सक्रिय कार्यक्रम',
        'Active Subjects': 'सक्रिय विषय',
        'Allocated Course': 'आवंटित कोर्स',
        'Allocated Educators': 'आवंटित शिक्षक',
        'Allocated Subject': 'आवंटित विषय',
        'Allocation List': 'आवंटन सूची',
        'Already have an account': 'क्या आपका खाता पहले से है',

        'An unexpected error occurred. Please try again':
            'एक अनपेक्षित त्रुटि हुई। कृपया फिर प्रयास करें',
        'Application Error': 'एप्लिकेशन त्रुटि',
        'Assigned Course': 'निर्धारित कोर्स',
        'Assigned Subject': 'निर्धारित विषय',
        'Class Average Benchmark': 'कक्षा औसत तुलना',
        'Class Benchmark Average': 'कक्षा तुलना औसत',
        'Click Here': 'यहाँ क्लिक करें',
        'Concurrent Departmental Audit across Courses, Subject Modules, Faculty Allocations & Student Enrollments':
            'कोर्स, विषय मॉड्यूल, शिक्षक आवंटन और विद्यार्थी नामांकन का विभागीय ऑडिट',
        'Course #': 'कोर्स नंबर',
        'Course ID': 'कोर्स आईडी',
        'Course Registrations': 'कोर्स पंजीकरण',
        'Course Total': 'कोर्स कुल',
        'Course-Wise Marksheet List': 'कोर्स-वार अंकपत्र सूची',
        'Courses Configured': 'कॉन्फ़िगर किए गए कोर्स',
        'Create a new account': 'नया खाता बनाएँ',
        'Create your ORS account': 'अपना ORS खाता बनाएँ',
        'Curriculum Modules': 'पाठ्यक्रम मॉड्यूल',
        'Date of Birth (MM/dd/yyyy)': 'जन्म तिथि (महीना/दिन/वर्ष)',
        'Delete Selected': 'चयनित हटाएँ',
        'Department Overview': 'विभाग का अवलोकन',
        'Duration / Term': 'अवधि / टर्म',
        'Enrolled Course': 'नामांकित कोर्स',
        'Enrolled Courses Found': 'नामांकित कोर्स मिले',
        'Enrolled Headcount': 'नामांकित विद्यार्थियों की संख्या',
        'Error Details': 'त्रुटि विवरण',
        'Faculty Directory & Teaching Loads': 'शिक्षक सूची और शिक्षण कार्यभार',
        'Faculty Email': 'शिक्षक ईमेल',
        'Faculty Teaching Allocations': 'शिक्षक शिक्षण आवंटन',
        'Filter Subject Ranking': 'विषय रैंकिंग फ़िल्टर करें',
        'Go Back': 'वापस जाएँ',
        'Go to Login': 'लॉगिन पर जाएँ',
        'Get Course-Wise Marksheet': 'कोर्स-वार अंकपत्र प्राप्त करें',
        'Insert title here': 'शीर्षक यहाँ डालें',
        'Linked Subjects': 'जुड़े हुए विषय',
        'Login here': 'यहाँ लॉगिन करें',
        'Lookup course-wise marksheet': 'कोर्स-वार अंकपत्र खोजें',
        'Manage academic courses': 'शैक्षणिक कोर्स प्रबंधित करें',
        'Manage college records': 'कॉलेज रिकॉर्ड प्रबंधित करें',
        'Manage course subjects': 'कोर्स विषय प्रबंधित करें',
        'Manage course-wise marksheets': 'कोर्स-वार अंकपत्र प्रबंधित करें',
        'Manage faculty records': 'शिक्षक रिकॉर्ड प्रबंधित करें',
        'Manage student records': 'विद्यार्थी रिकॉर्ड प्रबंधित करें',
        'Manage system users': 'सिस्टम उपयोगकर्ता प्रबंधित करें',
        'Manage user roles': 'उपयोगकर्ता भूमिकाएँ प्रबंधित करें',
        'Marks (Out of 100)': 'अंक (100 में से)',
        'Marks Obtained': 'प्राप्त अंक',
        'Max Marks': 'अधिकतम अंक',
        'No courses found': 'कोई कोर्स नहीं मिला',
        'No faculty allocations found': 'कोई शिक्षक आवंटन नहीं मिला',
        'No subject modules found': 'कोई विषय मॉड्यूल नहीं मिला',
        'Page Not Found': 'पृष्ठ नहीं मिला',
        'Password (8 Characters)': 'पासवर्ड (8 अक्षर)',
        'Peer identities are redacted for privacy protection (User Story 3.2)':
            'गोपनीयता के लिए सहपाठियों की पहचान छिपाई गई है',
        'Phone No': 'फोन नंबर',
        'Print DOC': 'DOC प्रिंट करें',
        'Print PDF': 'PDF प्रिंट करें',
        'Program Chair Departmental Audit & Enrollment Analysis':
            'प्रोग्राम चेयर विभागीय ऑडिट और नामांकन विश्लेषण',
        'Program Chair audit & enrollment metrics':
            'प्रोग्राम चेयर ऑडिट और नामांकन आँकड़े',
        'Search for a marksheet': 'अंकपत्र खोजें',
        'Sign Up': 'साइन अप',
        'Sign in to your account': 'अपने खाते में साइन इन करें',
        'Student & Course Roster List': 'विद्यार्थी और कोर्स सूची',
        'Student Academic Record': 'विद्यार्थी शैक्षणिक रिकॉर्ड',
        'Subject Class Average': 'विषय कक्षा औसत',
        'Subject ID': 'विषय आईडी',
        'Subject Status': 'विषय स्थिति',
        'Subject-Specific Merit List & Class Benchmarks':
            'विषय-विशिष्ट मेरिट सूची और कक्षा तुलना',
        'The page you are looking for doesn’t exist or has been moved':
            'आप जिस पृष्ठ को खोज रहे हैं वह मौजूद नहीं है या स्थानांतरित हो गया है',
        'View merit rankings': 'मेरिट रैंकिंग देखें',
        'View and edit your profile': 'अपनी प्रोफ़ाइल देखें और संपादित करें',

        'Registration': 'पंजीकरण',
        'Duration': 'अवधि',
        'Semester': 'सेमेस्टर',
        'Phone': 'फोन',
        'City': 'शहर',
        'State': 'राज्य',
        'Assigned': 'निर्धारित',
        'Active': 'सक्रिय',
        'Print': 'प्रिंट करें',
        'Manage': 'प्रबंधित करें',
        'People': 'लोग',
        'Modules': 'मॉड्यूल',
        'Lookup': 'खोजें',
        'Found': 'मिले',
        'Previous': 'पिछला',
        'Next': 'अगला',
        'Current': 'वर्तमान',
        'Open': 'खोलें',
        'Close': 'बंद करें',
        'Registration No': 'पंजीकरण नंबर',
        'Program': 'कार्यक्रम',
        'Record': 'रिकॉर्ड',
        'Records': 'रिकॉर्ड',
        'Reset': 'रीसेट',
        'Send': 'भेजें',
        'Create': 'बनाएँ',
        'Insert': 'जोड़ें',
        'Details': 'विवरण',
        'Error': 'त्रुटि'
    };

    const entries = Object.entries(translations)
        .sort((a, b) => b[0].length - a[0].length);

    function translateString(value) {
        if (!value) return value;

        const trimmed = value.trim().replace(/\s+/g, ' ');
        if (!trimmed) return value;

        let translated = translations[trimmed];

        if (!translated) {
            translated = trimmed;

            for (const [english, hindi] of entries) {
                if (english.length < 3) continue;

                if (/^[A-Za-z0-9]+$/.test(english)) {
                    const escaped = english.replace(
                        /[.*+?^${}()|[\]\\]/g, '\\$&'
                    );

                    const tokenPattern = new RegExp(
                        '(^|[^A-Za-z0-9_-])' + escaped +
                        '(?=$|[^A-Za-z0-9_-])',
                        'g'
                    );

                    translated = translated.replace(
                        tokenPattern,
                        function (match, prefix) {
                            return prefix + hindi;
                        }
                    );
                } else if (translated.includes(english)) {
                    translated = translated.split(english).join(hindi);
                }
            }

            translated = translated.replace(
                /^Welcome,\s*/,
                'स्वागत है, '
            );

            translated = translated.replace(
                /^Welcome to ORS\s*/,
                'ORS में आपका स्वागत है '
            );
        }

        if (translated === trimmed) return value;

        const leading = value.match(/^\s*/)[0];
        const trailing = value.match(/\s*$/)[0];

        return leading + translated + trailing;
    }

    function translateTextNodes(root) {
        const walker = document.createTreeWalker(
            root,
            NodeFilter.SHOW_TEXT,
            {
                acceptNode(node) {
                    if (!node.nodeValue || !node.nodeValue.trim()) {
                        return NodeFilter.FILTER_REJECT;
                    }

                    const parent = node.parentElement;

                    if (
                        !parent ||
                        /^(SCRIPT|STYLE|NOSCRIPT|TEXTAREA)$/i.test(
                            parent.tagName
                        )
                    ) {
                        return NodeFilter.FILTER_REJECT;
                    }

                    if (
                        parent.closest(
                            '[data-no-translate],script,style,textarea,code,pre'
                        )
                    ) {
                        return NodeFilter.FILTER_REJECT;
                    }

                    return NodeFilter.FILTER_ACCEPT;
                }
            }
        );

        let node;
        while ((node = walker.nextNode())) {
            node.nodeValue = translateString(node.nodeValue);
        }
    }

    function translateAttributes(root) {
        root.querySelectorAll(
            '[placeholder],[title],[aria-label],[alt]'
        ).forEach(function (element) {
            ['placeholder', 'title', 'aria-label', 'alt'].forEach(
                function (attribute) {
                    if (element.hasAttribute(attribute)) {
                        element.setAttribute(
                            attribute,
                            translateString(element.getAttribute(attribute))
                        );
                    }
                }
            );
        });

        /*
         * Keep the original operation value for the server.
         * Only translate the visible submit button label.
         */
        root.querySelectorAll(
            'input[type="submit"][name="operation"]'
        ).forEach(function (button) {
            const original = button.value;
            const display = translateString(original);

            if (display !== original) {
                button.dataset.orsOperationValue = original;
                button.value = display;

                if (
                    button.form &&
                    !button.form.dataset.orsLanguageSubmitHandler
                ) {
                    const form = button.form;
                    form.dataset.orsLanguageSubmitHandler = 'true';

                    form.addEventListener('submit', function () {
                        form.querySelectorAll(
                            'input[type="submit"][name="operation"]' +
                            '[data-ors-operation-value]'
                        ).forEach(function (submitButton) {
                            submitButton.value =
                                submitButton.dataset.orsOperationValue;
                        });
                    });
                }
            }
        });

        root.querySelectorAll(
            'input[type="button"],button:not([name="operation"])'
        ).forEach(function (button) {
            if (button.tagName === 'INPUT') {
                button.value = translateString(button.value);
            }
        });
    }

    function applyTranslations() {
        if (!document.body) return;

        translateTextNodes(document.body);
        translateAttributes(document);
    }

    applyTranslations();

    /*
     * Translate text inserted after the initial page load.
     */
    if (window.MutationObserver && document.body) {
        const observer = new MutationObserver(function (records) {
            records.forEach(function (record) {
                record.addedNodes.forEach(function (node) {
                    if (node.nodeType === Node.TEXT_NODE) {
                        node.nodeValue = translateString(node.nodeValue);
                    } else if (node.nodeType === Node.ELEMENT_NODE) {
                        translateTextNodes(node);
                        translateAttributes(node);
                    }
                });
            });
        });

        observer.observe(document.body, {
            childList: true,
            subtree: true
        });
    }
})();