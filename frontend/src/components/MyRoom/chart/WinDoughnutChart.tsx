import React, { useEffect, useState } from 'react';
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from 'chart.js';
import { Doughnut } from 'react-chartjs-2';

ChartJS.register(ArcElement, Tooltip, Legend);

interface ChartData {
  labels: String[];
  datasets: Array<any>;
}

type profileProps = {
  vicCnt : number[];
  totalVic : number;
}
function DoughnutChart({vicCnt, totalVic} : profileProps) {
    const [label, setLabel] = useState<String[]>(['캐치마인드', '몸으로 말해요', '너의 목소리가 들려'])
    const data = {
      labels: label,
      datasets: [
        {
          data: vicCnt,
          backgroundColor: [
            'rgba(255, 99, 132, 0.5)',
            'rgba(54, 162, 235, 0.5)',
            'rgba(255, 206, 86, 0.5)',
            'rgba(153, 102, 255, 0.5)',
            'rgba(255, 159, 64, 0.5)',
            'rgba(75, 192, 192, 0.5)',
          ],
          borderColor: [
            'rgba(255, 99, 132, 1)',
            'rgba(54, 162, 235, 1)',
            'rgba(255, 206, 86, 1)',
            'rgba(153, 102, 255, 1)',
            'rgba(255, 159, 64, 1)',
            'rgba(75, 192, 192, 1)',
          ],
          borderWidth: 1.5,
          hoverOffset: 4
        }
      ]
    }
  return <Doughnut 
          style={{cursor:"pointer"}}
          data={data} 
          options={{plugins:{legend:{display:true, labels:{color:'#121212', font:{family: "neodgm", size: 11}}}}}}
          />
}

export default DoughnutChart;
