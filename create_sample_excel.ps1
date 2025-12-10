# PowerShell Script to Create Sample Excel File for Testing
# This script creates a sample Excel file with student data

Write-Host "Creating sample Excel file for student import..." -ForegroundColor Green

# Sample student data with BURSAR FIELDS
$students = @(
    @{
        StudentID = ""  # Empty - will auto-generate
        FirstName = "Tatenda"
        LastName = "Moyo"
        DateOfBirth = "2010-03-15"
        Grade = "Grade 5"
        ParentName = "Grace Moyo"
        ParentPhone = "+263771234567"
        ParentEmail = "grace.moyo@email.com"
        Address = "123 Samora Machel Avenue, Harare"
        FeeCategory = "Day Scholar"
        TuitionFee = "500.00"
        BoardingFee = "0.00"
        DevelopmentLevy = "50.00"
        ExamFee = "30.00"
        PreviousBalance = "100.00"
        AmountPaid = "400.00"
        HasScholarship = "NO"
        ScholarshipAmount = "0.00"
        SiblingDiscount = "0.00"
        BursarNotes = "Good payment record"
    },
    @{
        StudentID = "STU002"
        FirstName = "Ruvimbo"
        LastName = "Ncube"
        DateOfBirth = "2011-07-22"
        Grade = "Grade 4"
        ParentName = "Tendai Ncube"
        ParentPhone = "+263772345678"
        ParentEmail = "tendai.ncube@email.com"
        Address = "456 Robert Mugabe Road, Bulawayo"
        FeeCategory = "Boarder"
        TuitionFee = "500.00"
        BoardingFee = "300.00"
        DevelopmentLevy = "50.00"
        ExamFee = "30.00"
        PreviousBalance = "0.00"
        AmountPaid = "880.00"
        HasScholarship = "NO"
        ScholarshipAmount = "0.00"
        SiblingDiscount = "0.00"
        BursarNotes = "Fully paid - boarder"
    },
    @{
        StudentID = ""  # Empty - will auto-generate
        FirstName = "Tinashe"
        LastName = "Mpofu"
        DateOfBirth = "2009-11-08"
        Grade = "Grade 6"
        ParentName = "Shuvai Mpofu"
        ParentPhone = "+263773456789"
        ParentEmail = "shuvai.mpofu@email.com"
        Address = "789 Herbert Chitepo Street, Mutare"
        FeeCategory = "Day Scholar"
        TuitionFee = "500.00"
        BoardingFee = "0.00"
        DevelopmentLevy = "50.00"
        ExamFee = "30.00"
        PreviousBalance = "200.00"
        AmountPaid = "0.00"
        HasScholarship = "YES"
        ScholarshipAmount = "250.00"
        SiblingDiscount = "50.00"
        BursarNotes = "Scholarship student - arrears from previous term"
    },
    @{
        StudentID = "STU004"
        FirstName = "Anesu"
        LastName = "Chikwanha"
        DateOfBirth = "2010-09-05"
        Grade = "Grade 5"
        ParentName = "Nyasha Chikwanha"
        ParentPhone = "+263774567890"
        ParentEmail = "nyasha.c@email.com"
        Address = "321 Josiah Tongogara Ave, Gweru"
        FeeCategory = "Day Scholar"
        TuitionFee = "500.00"
        BoardingFee = "0.00"
        DevelopmentLevy = "50.00"
        ExamFee = "30.00"
        PreviousBalance = "0.00"
        AmountPaid = "200.00"
        HasScholarship = "NO"
        ScholarshipAmount = "0.00"
        SiblingDiscount = "25.00"
        BursarNotes = "Sibling discount - partial payment made"
    },
    @{
        StudentID = ""  # Empty - will auto-generate
        FirstName = "Tadiwanashe"
        LastName = "Sibanda"
        DateOfBirth = "2011-01-17"
        Grade = "Grade 4"
        ParentName = "Kudzai Sibanda"
        ParentPhone = "+263775678901"
        ParentEmail = "kudzai.sibanda@email.com"
        Address = "654 Jason Moyo Street, Masvingo"
        FeeCategory = "Boarder"
        TuitionFee = "500.00"
        BoardingFee = "300.00"
        DevelopmentLevy = "50.00"
        ExamFee = "30.00"
        PreviousBalance = "0.00"
        AmountPaid = "0.00"
        HasScholarship = "NO"
        ScholarshipAmount = "0.00"
        SiblingDiscount = "0.00"
        BursarNotes = "New boarder - no payments yet"
    }
)

# Create CSV file (can be opened and saved as Excel)
$csvPath = "$PSScriptRoot\students_sample.csv"

Write-Host "Generating CSV file..." -ForegroundColor Yellow

$csvContent = "Student ID,First Name,Last Name,Date of Birth,Grade,Parent Name,Parent Phone,Parent Email,Address,Fee Category,Tuition Fee,Boarding Fee,Development Levy,Exam Fee,Previous Balance,Amount Paid,Has Scholarship,Scholarship Amount,Sibling Discount,Bursar Notes`n"

foreach ($student in $students) {
    $csvContent += "$($student.StudentID),$($student.FirstName),$($student.LastName),$($student.DateOfBirth),$($student.Grade),$($student.ParentName),$($student.ParentPhone),$($student.ParentEmail),$($student.Address),$($student.FeeCategory),$($student.TuitionFee),$($student.BoardingFee),$($student.DevelopmentLevy),$($student.ExamFee),$($student.PreviousBalance),$($student.AmountPaid),$($student.HasScholarship),$($student.ScholarshipAmount),$($student.SiblingDiscount),$($student.BursarNotes)`n"
}

$csvContent | Out-File -FilePath $csvPath -Encoding UTF8

Write-Host "CSV file created: $csvPath" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Open $csvPath in Excel" -ForegroundColor White
Write-Host "2. Save As > Excel Workbook (.xlsx)" -ForegroundColor White
Write-Host "3. Upload to http://localhost:8080/api/students/upload-excel" -ForegroundColor White
Write-Host ""
Write-Host "Sample students included:" -ForegroundColor Cyan
Write-Host "- 5 students from different cities in Zimbabwe" -ForegroundColor White
Write-Host "- 3 without Student IDs (will auto-generate)" -ForegroundColor White
Write-Host "- 2 with predefined IDs (STU002, STU004)" -ForegroundColor White
Write-Host "- Mix of grades: 4, 5, and 6" -ForegroundColor White
Write-Host "- 2 Boarders, 3 Day Scholars" -ForegroundColor White
Write-Host "- 1 Scholarship student" -ForegroundColor White
Write-Host "- Various payment statuses: Paid, Partially Paid, Arrears" -ForegroundColor White
Write-Host ""
Write-Host "Happy testing!" -ForegroundColor Green
