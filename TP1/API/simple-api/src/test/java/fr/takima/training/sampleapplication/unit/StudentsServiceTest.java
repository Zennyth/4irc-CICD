package fr.takima.training.sampleapplication.unit;

import fr.takima.training.simpleapi.dao.StudentDAO;
import fr.takima.training.simpleapi.entity.Student;
import fr.takima.training.simpleapi.entity.Department;
import fr.takima.training.simpleapi.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class StudentsServiceTest {

    @InjectMocks
    private StudentService studentService;

    @Mock
    private StudentDAO studentDAO;

    private Department department = Department.builder().id(1L).name("DepartementTest").build();
    private Student student = Student
            .builder()
            .id(1L)
            .firstname("Firstname")
            .lastname("lastname")
            .department(department)
            .build();

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
    }


    @Test
    public void testGetStudentById() {
        when(studentDAO.getById(1L)).thenReturn(student);
        assertEquals(student, studentService.getStudentById(1L));
    }

    @Test
    public void testGetStudentByIdWithNegativeId() {
        assertThrows(IllegalArgumentException.class, () -> studentService.getStudentById(-5));
    }

    @Test
    public void testGetStudentsByDepartmentName() {
        List<Student> students = new ArrayList<>();
        students.add(student);
        when(studentDAO.findStudentsByDepartment_Name("DepartmentTest")).thenReturn(students);

        assertEquals(students, studentService.getStudentsByDepartmentName("DepartmentTest"));
    }

    @Test
    public void testGetStudentsByDepartmentNameWithNullValue() {
        assertThrows(IllegalArgumentException.class, () -> studentService.getStudentsByDepartmentName(null));
    }

    @Test
    public void testGetStudentsByDepartmentNameWithEmptyValue() {
        assertThrows(IllegalArgumentException.class, () -> studentService.getStudentsByDepartmentName(""));
    }

    @Test
    public void testGetStudentsNumberByDepartmentName() {
        when(studentDAO.countAllByDepartment_Name("DepartmentTest")).thenReturn(1);
        assertEquals(1, studentService.getStudentsNumberByDepartmentName("DepartmentTest"));
    }

    @Test
    public void testGetStudentsNumberByDepartmentNameWithNullValue() {
        assertThrows(IllegalArgumentException.class, () -> studentService.getStudentsNumberByDepartmentName(null));
    }

    @Test
    public void testGetStudentsNumberByDepartmentNameWithEmptyValue() {
        assertThrows(IllegalArgumentException.class, () -> studentService.getStudentsNumberByDepartmentName(null));
    }

    @Test
    public void testAddStudent() {
        when(studentDAO.save(student)).thenReturn(student);
        assertEquals(student, studentService.addStudent(student));
    }

    @Test
    public void testAddStudentWithBadLastname() {
        Student studentWithNullLastname = Student.builder().id(1L).firstname("abc").department(department).build();
        assertThrows(IllegalArgumentException.class, () -> studentService.addStudent(studentWithNullLastname));

        Student studentWithEmptyLastname = Student.builder().id(1L).firstname("abc").lastname("").department(department).build();
        assertThrows(IllegalArgumentException.class, () -> studentService.addStudent(studentWithEmptyLastname));
    }

    @Test
    public void testAddStudentWithoutDepartment() {
        Student studentWithoutDepartment = Student.builder().id(1L).lastname("abc").build();
        assertThrows(IllegalArgumentException.class, () -> studentService.addStudent(studentWithoutDepartment));
    }

    @Test
    public void testRemoveStudentById() {
        assertDoesNotThrow(() -> studentService.removeStudentById(1L));
    }

    @Test
    public void testRemoveStudentWithNegativeId() {
        assertThrows(IllegalArgumentException.class, () -> studentService.removeStudentById(-5));
    }
}
