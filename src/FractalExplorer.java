import javax.imageio.ImageIO;
import javax.swing.*;
//Swing
//предоставляет абстрактный класс javax.swing.SwingWorker, который облегчает процесс
//организации фонового потока
//SwingWorker <T, V>. Тип T - это тип значения, возвращаемого функцией
//doInBackground(), когда задача полностью выполнена. Тип V используется,
//когда фоновая задача возвращает промежуточные значения во время
//выполнения; эти промежуточные значения будут доступны при использовании
//методов publish () и process (). Оба типа могут не использоваться, в таких
//случаях необходимо указать Object для неиспользуемого типа
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Objects;

public class FractalExplorer {
    /** Целое число «размер экрана», которое является шириной и высотой
     отображения в пикселях **/
    private int size;

    /** Ссылка JImageDisplay, для обновления отображения в разных
     методах в процессе вычисления фрактала **/
    private JImageDisplay jDisplay;

    /** Будет использоваться ссылка на базовый
     класс для отображения других видов фракталов в будущем **/
    private FractalGenerator fractal;

    /** Объект Rectangle2D.Double, указывающий диапазона комплексной
     плоскости, которая выводится на экран **/
    private Rectangle2D.Double range;

    /** вынесение кнопок: выбора фрактала, сброса и сохранения
     * на один уровень абстракции выше, чтобы к ним могли обращаться несколько методов **/
    JComboBox comboBox;
    JButton resetDisplay;
    JButton saveImage;

    /** флаг отслеживающий количество оставшихся строк, во время прорисовки **/
    private int rowsLost;

    public FractalExplorer(int display_size) {
        size = display_size;
        range = new Rectangle2D.Double();
        fractal = new Mandelbrot();
        fractal.getInitialRange(range);
        jDisplay = new JImageDisplay(display_size, display_size);
    }

    /** метод createAndShowGUI (), инициализирует
     графический интерфейс Swing: JFrame, содержащий объект JImageDisplay, и
     кнопку для сброса отображения **/
    public void createAndShowGUI () {
        //создание окна
        JFrame frame = new JFrame("Fractal Explorer");

        //добавление изображения в центр окна
        jDisplay.setLayout(new BorderLayout());
        frame.add(jDisplay, BorderLayout.CENTER);

        //запись кнопок сохранения и сброса изображения
        saveImage = new JButton("Save Image");
        resetDisplay = new JButton("Reset Display");

        //добавление панели кнопок сохранения и сброса в нижнюю часть дисплея
        JPanel panelButton = new JPanel();
        panelButton.add(saveImage);
        panelButton.add(resetDisplay);
        frame.add(panelButton, BorderLayout.SOUTH);

        //добавление реакции на нажатие кнопки сброса
        InActionListener clearAction = new InActionListener();
        resetDisplay.addActionListener(clearAction);

        //добавление реакции на нажатие дисплея
        InMouseListener mouseListener = new InMouseListener();
        jDisplay.addMouseListener(mouseListener);

        //добавление выхода из окна по умолчанию
        frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);

        //создание таблицы фракталов и панели выбора одного из представленных
        String[] names = {"Mandelbrot", "Tricorn", "Burning Ship"};
        comboBox = new JComboBox(names);
        JLabel label = new JLabel("Fractal: ");
        JPanel panelBox = new JPanel();

        //добавление панели фракталов в верхнюю часть окна
        panelBox.add(label);
        panelBox.add(comboBox);
        frame.add(panelBox, BorderLayout.NORTH);

        //добавление реакции на выбор фрактала
        ChooseButtonHandler chooseAction = new ChooseButtonHandler();
        comboBox.addActionListener(chooseAction);

        //добавление реакции на сохранение изображения
        SaveImageButton saveAction = new SaveImageButton();
        saveImage.addActionListener(saveAction);

        //Данные операции правильно разметят содержимое окна, сделают его
        //видимым (окна первоначально не отображаются при их создании для того,
        //чтобы можно было сконфигурировать их прежде, чем выводить на экран), и
        //затем запретят изменение размеров окна
        frame.pack();
        frame.setVisible(true);
        frame.setResizable(false);
    }

    /** Замена реализации метода прорисовки фрактала, теперь рисование идёт
     * фоновым потоком, чтобы основной поток не мешал прорисовывать изображение
     * мы отключаем кнопки приложения и для
     * каждой строки в отображении создаётся отдельный рабочий объект, а затем
     * вызывается для него метод execute(). Это действие запустит фоновый поток и
     * запустит задачу в фоновом режиме. **/
    private void drawFractal () {
        enableIO(false);
        rowsLost = size;
        for (int y = 0; y < size; y++){
            FractalWorker fractal = new FractalWorker(y);
            fractal.execute();
        }
    }

    /** внутренний класс для обработки событий
     java.awt.event.ActionListener от кнопки сброса **/
    public class InActionListener implements ActionListener {
        @Override
        public void actionPerformed (ActionEvent event) {
            fractal.getInitialRange(range);
            drawFractal();
        }
    }
    /** внутренний класс для обработки событий
     java.awt.event.MouseListener с дисплея **/
    private class InMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent event) {
            //если фрактал ещё не прорисован,
            // метод ничего не выполняет
            if (rowsLost != 0) return;

            int x = event.getX();
            double xCord = fractal.getCoord(range.x, range.x+range.width, size,x);

            int y = event.getY();
            double yCord = fractal.getCoord(range.y, range.y+range.height, size,y);

            fractal.recenterAndZoomRange(range, xCord, yCord, 0.5);
            // При получении события о щелчке мышью, класс
            //отображает пиксельные координаты щелчка в область фрактала, а затем вызывает
            //метод генератора recenterAndZoomRange() с координатами, по которым
            //щелкнули, и масштабом 0.5
            drawFractal();
        }
    }

    /** Реализация класса реакции на выбор фрактала **/
    public class ChooseButtonHandler implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            JComboBox combo = (JComboBox) event.getSource();
            String name = (String) combo.getSelectedItem();
            switch (Objects.requireNonNull(name)) {
                case ("Mandelbrot") -> fractal = new Mandelbrot();
                case ("Tricorn") -> fractal = new Tricorn();
                case ("Burning Ship") -> fractal = new BurningShip();
            }
            fractal.getInitialRange(range);
            drawFractal();
        }
    }

    /** Реализация класса реакции на сохранение изображения **/
    public class SaveImageButton implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            //настройка средства выбора файлов, чтобы
            //изображения сохранялись только в формате PNG
            JFileChooser chooser = new JFileChooser();
            FileFilter filter = new FileNameExtensionFilter("PNG Images", "png");
            chooser.setFileFilter(filter);
            chooser.setAcceptAllFileFilterUsed(false);

            //сохранение фрактала на диск
            int result = chooser.showSaveDialog(jDisplay);
            if (result == JFileChooser.APPROVE_OPTION) {
                File dir = chooser.getSelectedFile();
                String dirString = dir.toString();
                //обработка исключений метода write()
                try{
                    BufferedImage image = jDisplay.getImage();
                    ImageIO.write(image, "png", dir);
                }
                catch(Exception exception){
                    JOptionPane.showMessageDialog(chooser, exception.getMessage(),"Can not save image", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    /** Функция, которая включает или отключает
     * кнопки с выпадающим списком в пользовательском
     * интерфейсе на основе указанного параметра. **/
    private void enableIO(boolean val){
        //Для включения или отключения
        //этих компонентов используется метод Swing setEnabled(boolean)
        comboBox.setEnabled(val);
        resetDisplay.setEnabled(val);
        saveImage.setEnabled(val);
    }

    /** Класс FractalWorker отвечает за вычисление значений цвета для
     одной строки фрактала, поэтому ему добавляются два поля: целочисленная y - координата вычисляемой строки, и массив чисел типа int для хранения
     вычисленных значений RGB для каждого пикселя в этой строке. **/
    private class FractalWorker extends SwingWorker<Object,Object>{
        private int y;
        private int[] valuesRGB;

        public FractalWorker (int y){
            this.y = y;
        }

        /** Метод, который фактически выполняет фоновые
         операции. Swing вызывает этот метод в фоновом потоке, а не в потоке
         обработки событий **/
        @Override
        protected Object doInBackground() {
            // Вместо того чтобы рисовать изображение в окне, цикл
            // сохраняет каждое значение RGB в соответствующем элементе
            // целочисленного массива
            valuesRGB = new int[size];
            for (int x = 0; x < valuesRGB.length; x ++) {
                double xCord = fractal.getCoord(range.x,range.x + range.width, size, x);
                double yCord = fractal.getCoord(range.y, range.y + range.height, size, y);
                int iterations = fractal.numIterations(xCord,yCord);
                int rgbColor;
                if (iterations == -1)
                    rgbColor = 0;
                else {
                    float hue = 0.7f + (float) iterations / 200f;
                    rgbColor = Color.HSBtoRGB(hue, 1f, 1f);
                }
                valuesRGB[x] = rgbColor;
            }
            return null;
        }

        /** Этот метод вызывается, когда фоновая задача завершена. Он
         вызывается в потоке обработки событий, поэтому данному методу разрешено
         взаимодействовать с пользовательским интерфейсом **/
        @Override
        protected void done() {
            //в методе перебираю массив строк данных, рисуя пиксели, которые
            //были вычислены в doInBackground ()
            for (int i = 0; i < valuesRGB.length; i ++) {
                jDisplay.drawPixel(i,y,valuesRGB[i]);
            }
            //Поскольку
            //изменил только одну строку, перерисовывать изображение целиком будет
            //затратно, поэтому использую метод JComponent.repaint(), который
            //позволит указать область для перерисовки
            jDisplay.repaint(0,0,y,size,1);

            //уменьшаем количество строк, которых надо прорисовать,
            //чтобы в конце включить кнопки
            rowsLost--;
            if (rowsLost == 0)
                enableIO(true);
        }
    }



    public static void main (String[] args) {

        FractalExplorer display = new FractalExplorer(800);
        display.createAndShowGUI();
        display.drawFractal();
    }
}